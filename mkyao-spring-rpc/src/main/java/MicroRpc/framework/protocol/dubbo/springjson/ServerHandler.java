package MicroRpc.framework.protocol.dubbo.springjson;

import MicroRpc.framework.beans.Invoker;
import MicroRpc.framework.protocol.dubbo.springjson.initializer.BysProtocol;
import MicroRpc.framework.redis.Registry.LocalRegistry;
import MicroRpc.framework.tools.serializable.BytesUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static MicroRpc.framework.protocol.dubbo.springjson.SecretKeys.NULL_RESULT;

@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<BysProtocol>  {
    private final ConcurrentHashMap<String,Object>instanceBuffer=new ConcurrentHashMap<>(64);
    private final ConfigurableListableBeanFactory beanFactory;
    private static final ConcurrentHashMap<String, ChannelHandlerContext> ctxMap=new ConcurrentHashMap<>(16);

    public static void addCtx(String ctxName,ChannelHandlerContext ctx){
        ctxMap.put(ctxName,ctx);
    }
    public static ChannelHandlerContext getCtx(String ctxName){
        return ctxMap.getOrDefault(ctxName,null);
    }

    private AtomicLong ggprotobufErrorCnt=new AtomicLong(1);


    public ServerHandler(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelRemoteName = ctx.channel().remoteAddress().toString();
        // TODO: 2023/8/9 还需要获取到客户端的服务名 (appName)
        // 获取到之后，将这个服务名添加上对应的连接地址(ctx)
        //RedisRegistry.addCtx(appName,channelRemoteName)
        addCtx(channelRemoteName,ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BysProtocol bysProtocol) throws Exception {
        byte[] content = bysProtocol.getContent();
        Invoker invoker = (Invoker) BytesUtils.ByteToObject(content);
//        if (BindInvoker.interfaceName.equals(invoker.getInterfaceName()) && BindInvoker.methodName.equals(invoker.getMethodName())
//        &&invoker.getParamsType()==null &&invoker.getParams()==null){
//            log.info("建立连接: {}",ctx.channel().remoteAddress());
//            return;
//        }
        //这里的obj需要从spring容器里面取而不能直接实例化
        Object o;
        String interfaceName = invoker.getInterfaceName();
        Class impl = LocalRegistry.getImpl(interfaceName);
        if (beanFactory==null){
            if (!instanceBuffer.contains(interfaceName)){
                o = impl.newInstance();
                instanceBuffer.put(interfaceName,o);
            }else {
                o=instanceBuffer.get(interfaceName);
            }
        }else {
            try {
                o = beanFactory.getBean(impl);
            } catch (BeansException e) {
                if (!instanceBuffer.contains(interfaceName)){
                    o = impl.newInstance();
                    instanceBuffer.put(interfaceName,o);
                }else {
                    o=instanceBuffer.get(interfaceName);
                }
            }
        }
        Method method = impl.getMethod(invoker.getMethodName(), invoker.getParamsType());
        Object result = method.invoke(o, invoker.getParams());
        if (result==null){
            result=NULL_RESULT;
        }
        byte[] bytes = BytesUtils.ObjectToByte(result);
        BysProtocol msg=new BysProtocol();
        msg.setContent(bytes);
        msg.setLen(bytes.length);
        ctx.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        BysProtocol error=new BysProtocol();
        byte[] bytes = BytesUtils.ObjectToByte(cause);
        error.setContent(bytes);
        error.setLen(bytes.length);
        ctx.writeAndFlush(error);
        cause.printStackTrace();
//        cause.printStackTrace();
        /*if (cause instanceof DecoderException ){
            if (cause.getMessage().startsWith("com.google.protobuf.InvalidProtocolBufferException")){
                log.info("【可无视】谷歌protobuf解码又又又出错了...第{}次",ggprotobufErrorCnt.getAndIncrement());
            }
        }else {
            cause.printStackTrace();
            log.error("当前已关闭连接!!!");
            ctx.close();
        }*/


    }
}
