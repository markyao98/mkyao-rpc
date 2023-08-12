package MicroRpc.framework.protocol.dubbo.springjson;

import MicroRpc.framework.beans.Invoker;
import MicroRpc.framework.beans.Url;
import MicroRpc.framework.protocol.dubbo.springjson.initializer.BysProtocolDecoder;
import MicroRpc.framework.protocol.dubbo.springjson.initializer.BysProtocolEncoder;
import MicroRpc.framework.redis.Registry.core.RedisRegistry;
import MicroRpc.framework.tools.Sleepers;
import MicroRpc.framework.tools.StringUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static MicroRpc.framework.protocol.dubbo.springjson.SecretKeys.CLOSE_CHANNEL;

@Slf4j(topic = "m.DubboClient")
public class DubboClient {

    private final Map<String, DubboClientHandler>urlHadnlers=new ConcurrentHashMap<>(16);
    private final Object syncLock=new Object();
    private Thread dobind=null;
    private final static int WAIT_FOR_BIND_MILLS=1000;


    private void dobind(Url url, DubboClientHandler cli)  {
        EventLoopGroup group;
        Bootstrap bootstrap;
        if (StringUtils.hasText(url.getHost()) && url.getPort()!=null){
            group=new NioEventLoopGroup();
            bootstrap=new Bootstrap();
            bootstrap
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new BysProtocolEncoder());
//                            pipeline.addLast(new ProtobufEncoder()); //proto编码器
                            pipeline.addLast(new BysProtocolDecoder());
                            pipeline.addLast(cli);
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect(url.getHost(), url.getPort());//sync的作用是阻塞,意思是让连接阻塞
            log.info("连接成功 {}",url);
            RedisRegistry.registConnect(url);
        }
    }
    private void connect2Server(Url url) {
        String urlkey = getUrlkey(url);
        DubboClientHandler clientHandler = new DubboClientHandler();
        urlHadnlers.put(urlkey,new DubboClientHandler());
        synchronized (urlkey.intern()){
            log.info("客户端连接rpc服务器...");
            dobind(url,clientHandler);
            log.info("客户端连接rpc服务器成功...");
            urlHadnlers.put(urlkey,clientHandler);
            Sleepers.sleep0(WAIT_FOR_BIND_MILLS);
        }
    }


    public Object send(Url url, Invoker invoker,int waitSec){
        String urlkey = getUrlkey(url);
        if (!urlHadnlers.containsKey(urlkey)){
            connect2Server(url);
        }
        synchronized (syncLock){
            Object result = null;
            DubboClientHandler dubboClientHandler = urlHadnlers.get(urlkey);
            try {
                dubboClientHandler.setInvoker(invoker);
            } catch (NullPointerException e) {
                log.error("连接出错,将会尝试重连..{}",urlkey);
                closeConnect(url);
                return null;
            }
            result = dubboClientHandler.getResult(waitSec);
            if (result!=null && CLOSE_CHANNEL.equals(result.toString())){
                log.error("连接已关闭,将会尝试重连..{}",urlkey);
                closeConnect(url);

            }
            return result;
        }
    }



    private String getUrlkey(Url url){
        return url.getHost()+":"+url.getPort();
    }

    /**
     * 关闭连接
     * @param url
     */
    public void killConnect(Url url){
        String urlKey=getUrlkey(url);
        if (urlKey!=null){
            closeConnect(url);
            RedisRegistry.killConnect(urlKey);
        }
    }

    public void closeConnect(Url url){
        String urlkey = getUrlkey(url);
        if (urlHadnlers.containsKey(urlkey)){
            DubboClientHandler dubboClientHandler = urlHadnlers.get(urlkey);
            ChannelHandlerContext context = dubboClientHandler.getContext();
            if (context!=null){
                context.close();
                log.info("{} 断开连接: {}",context.channel().localAddress(),urlkey);
            }
            urlHadnlers.remove(urlkey);
        }
    }

    /**
     * 重连接
     * @param url
     */
    public void reConnect(Url url){
        closeConnect(url);
        connect2Server(url);
    }

    /**
     * 初始化所有连接
     */
    public void initConnect(){
        List<Url> initConnects=RedisRegistry.getInitConnects();
        int size = initConnects.size();
        if (size>0){
            for (Url url : initConnects) {
                connect2Server(url);
            }
        }
    }

}
