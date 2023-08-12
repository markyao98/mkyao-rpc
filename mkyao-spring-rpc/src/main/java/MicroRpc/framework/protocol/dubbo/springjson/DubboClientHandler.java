package MicroRpc.framework.protocol.dubbo.springjson;

import MicroRpc.framework.beans.Invoker;
import MicroRpc.framework.beans.Url;
import MicroRpc.framework.excption.DubboException;
import MicroRpc.framework.protocol.dubbo.springjson.initializer.BysProtocol;
import MicroRpc.framework.redis.Registry.core.RedisRegistry;
import MicroRpc.framework.tools.serializable.BytesUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

import static MicroRpc.framework.protocol.dubbo.springjson.SecretKeys.CLOSE_CHANNEL;

@Slf4j(topic = "m.DubboClientHandler")
public class DubboClientHandler extends SimpleChannelInboundHandler<BysProtocol> {
    private Invoker invoker;
    private Object result;
    private ChannelHandlerContext context;


    public void setInvoker(Invoker invoker) {
        this.invoker=invoker;
        //处理multiFile
        //class org.springframework.web.multipart.support.StandardMultipartHttpServletRequest$StandardMultipartFile
        result=null;
        BysProtocol bysProtocol=new BysProtocol();
        byte[] bytes = BytesUtils.ObjectToByte(invoker);
        bysProtocol.setContent(bytes);
        bysProtocol.setLen(bytes.length);
        context.writeAndFlush(bysProtocol);
    }

    public ChannelHandlerContext getContext() {
        return context;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        context=ctx;
    }

    @Override
    protected synchronized void channelRead0(ChannelHandlerContext channelHandlerContext, BysProtocol msg) throws Exception {
        byte[] bytes = msg.getContent();
        //todo 直接将bytes序列化成Object?
        Object object = BytesUtils.ByteToObject(bytes);
        this.result=object;
//        String r=new String(bytes,StandardCharsets.UTF_8);
//        this.result=r;
        notifyAll();
    }

    public synchronized Object getResult(int waitSec) {
        if (null==result){
            try {
                wait(waitSec*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    @Override
    public synchronized void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        shutdownCurrService(socketAddress);
        ctx.close();
        notifyAll();
        throw new DubboException("与主机的连接异常==>"+ socketAddress);
    }

    @Override
    public synchronized void channelInactive(ChannelHandlerContext ctx) throws Exception {
        result=CLOSE_CHANNEL;
        notifyAll();
    }

    private void shutdownCurrService(SocketAddress socketAddress){
        String s = socketAddress.toString();
        String urls = s.split("\\/")[1];
        String host = urls.split(":")[0];
        String port = urls.split(":")[1];
        Url url=new Url(host,Integer.parseInt(port));
        RedisRegistry.adviceError(url,invoker.getInterfaceName());
    }



}
