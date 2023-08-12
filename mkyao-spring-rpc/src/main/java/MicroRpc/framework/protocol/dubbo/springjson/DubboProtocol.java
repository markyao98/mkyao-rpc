package MicroRpc.framework.protocol.dubbo.springjson;

import MicroRpc.framework.beans.Invoker;
import MicroRpc.framework.beans.Url;
import MicroRpc.framework.commons.DemotePolicy;
import MicroRpc.framework.loadbalance.LoadBalance;
import MicroRpc.framework.protocol.handler.AbstractProtocolHandler;
import MicroRpc.framework.redis.Registry.core.RedisRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//json版本
@Slf4j(topic = "m.DubboProtocol")
public class DubboProtocol extends AbstractProtocolHandler {
    private final int MAX_CLINENT_CAPACITY=4;
    private final Thread lifeThread=new Thread(new LifeRunnable());

    private final List<DubboClient>dubboClientPools=new ArrayList<>(MAX_CLINENT_CAPACITY);

    private final Object clientLock=new Object();
    private volatile int clientRet=0;
    private final ObjectMapper objectMapper=new ObjectMapper();
    public final static String ERROR_MSG="目标url已断开连接,为你重新分配合适的服务:";
    public DubboProtocol(LoadBalance loadBalance) {
        super(loadBalance);
        for (int i = 0; i < MAX_CLINENT_CAPACITY; i++) {
            DubboClient dubboClient = new DubboClient();
            dubboClientPools.add(dubboClient);
        }
        lifeThread.start();
//        log.info("建立连接中...");
//        for (int i = 0; i < MAX_CLINENT_CAPACITY; i++) {
//            send(new Invoker(BindInvoker.interfaceName,BindInvoker.methodName,null,null),1);
//        }
    }

    @Override
    public void init(){
        log.info("开始预热连接...");
        for (DubboClient dubboClient : dubboClientPools) {
            dubboClient.initConnect();
        }
        log.info("预热完毕~");
    }
    public synchronized void closeClient2Server(Url url){
        for (DubboClient dubboClient : dubboClientPools) {
            dubboClient.closeConnect(url);
        }
    }

    public synchronized void reConnect2Server(Url url){
        for (DubboClient dubboClient : dubboClientPools) {
            dubboClient.reConnect(url);
        }
    }

    @Override
    public Object send(Invoker invoker,int waitSec,DemotePolicy demotePolicy, Object fallbk, Method fallbkMethod,int demoteRetryCnt) {
        Url url = loadBalance.selectUrl(invoker);
        if (url==null){
            log.error("当前服务提供者列表没有对应的服务.");
            return null;
        }
        Object result=null;
        int retry=0;
        synchronized (clientLock){
            clientRet=(clientRet+1)%MAX_CLINENT_CAPACITY;
        }
        DubboClient dubboClient=dubboClientPools.get(clientRet);
        while (retry<3){
            result = dubboClient.send(url, invoker,waitSec);
            if (result==null){
                log.info("重试中... {}",retry);
                retry++;
            }else {
                break;
            }
        }
        if (result instanceof Throwable){
            log.error("服务提供方出错了!!! {}", ((Throwable) result).getCause().getMessage());
            if (demotePolicy== DemotePolicy.RETRY){
                log.info("重试策略,即将开始重试..");
                retry=1;
                while (retry<=demoteRetryCnt){
                    result = dubboClient.send(url, invoker,waitSec);
                    if (result instanceof Throwable){
                        log.info("重试中... {}",retry);
                        retry++;
                    }else {
                        break;
                    }
                }
                if (result instanceof Throwable){
                    log.error("服务重试失败!");
                    result=null;
                }else {
                    log.info("服务重试成功!");
                }
            }else if (demotePolicy==DemotePolicy.DEMOTE_FALLBACK){
                log.info("回调策略,即将执行回调方法..");
                try {
                    result=fallbkMethod.invoke(fallbk,invoker.getParams());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                log.info("回调方法已执行...{} ",fallbkMethod.getName());
            }

        }

        if (result!=null && SecretKeys.NULL_RESULT.equals(result.toString())){
            result=null;
        }
        return result;
    }


    @Override
    public void recv(Url url, ConfigurableListableBeanFactory beanFactory) {
        NettyServer.serverStart0(url,beanFactory);
    }

    @Override
    public void recv(Url url) {
        NettyServer.serverStart0(url,null);
    }


    //检查连接的连接性
    private class LifeRunnable implements Runnable{
        @Override
        public void run() {
            while (true){
                Url[] kills=RedisRegistry.hasKills();
                Url[] reConnects=RedisRegistry.hasReconnects();
                if (reConnects!=null && reConnects.length>0){
                    for (Url url : reConnects) {
                        if (url!=null){
                            reConnect2Server(url);
                        }
                    }
                    RedisRegistry.reconnectsOk();
                }
                else if (kills!=null && kills.length>0){
                    for (Url url : kills) {
                        closeClient2Server(url);
                    }
                    RedisRegistry.killOk();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
