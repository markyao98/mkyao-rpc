package MicroRpc.framework.protocol.handler;

import MicroRpc.framework.beans.Invoker;
import MicroRpc.framework.commons.DemotePolicy;
import MicroRpc.framework.loadbalance.LoadBalance;
import MicroRpc.framework.protocol.Protocol;
import MicroRpc.framework.tools.beanutils.MapToObjectConverter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j(topic = "m.AbstractProtocolHandler")
public abstract class AbstractProtocolHandler implements Protocol {
    protected final LoadBalance loadBalance;
    public AbstractProtocolHandler(LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }
    private final Map<String,Object>fallbackBeans=new ConcurrentHashMap<>(256);
    private final Map<String,Method>fallbackMethods=new ConcurrentHashMap<>(256);

    @Override
    public abstract Object send(Invoker invoker,int waitSec,
                                DemotePolicy demotePolicy, Object fallbk, Method fallbkMethod,int retryCnt);

    protected <T>T getProxy(Class clazz,int waitSec, DemotePolicy demotePolicy,int retryCnt) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class[]{clazz},
                (proxy, method, args) -> {
                    Object fallbk=null;
                    Method fallbkMethod=null;
                    Class[] argsTypes = buildTypes(args);
                    if (demotePolicy==DemotePolicy.DEMOTE_FALLBACK){
                        String key = getInvokerKey(method,argsTypes);
                        fallbk=fallbackBeans.get(key);
                        fallbkMethod = fallbackMethods.get(key);
                    }
                    Invoker invoker = new Invoker(clazz.getName(), method.getName(), argsTypes, args);
                    Object result = send(invoker,waitSec,demotePolicy,fallbk,fallbkMethod,retryCnt);
                    // TODO: 2023/6/14 解决由map无法转化为object的bug
                    if (result!=null && result instanceof Map){
                        Class<?> returnType = method.getReturnType();
                        result= MapToObjectConverter.convert((Map<String, Object>) result,returnType);
                    }
                    return result;
                }
        );
    }

    protected String getInvokerKey(Method method, Class[] argsType){
        StringBuilder sb=new StringBuilder(method.getName()+"#");
        if (argsType==null ||argsType.length==0){
            return sb.toString();
        }
        for (Class argtype : argsType) {
            sb.append(argtype.getName());
        }
        return sb.toString();
    }

    private Class[] buildTypes(Object[] args) {
        if (null == args || args.length==0) {
            return null;
        }
        Class[] cs=new Class[args.length];
        int i=0;
        for (Object a : args) {
            cs[i++]=a.getClass();
        }
        return cs;
    }

    @Override
    public <T> T getService(String interfaceName,int waitSec){
        Class clazz = null;
        try {
            clazz = ClassLoader.getSystemClassLoader().loadClass(interfaceName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return getService(clazz,waitSec);
    }

    @Override
    public <T> T getService(Class clazz,int waitSec){
        return getService(clazz,waitSec,DemotePolicy.RETRY,null,3);

    }

    @Override
    public <T> T getService(Class clazz, int waitSec, DemotePolicy demotePolicy, Class fallback,int retryCnt) {
        if (demotePolicy==DemotePolicy.RETRY || fallback==null){
            return getProxy(clazz,waitSec,DemotePolicy.RETRY,retryCnt);
        }
        Object fallbkBean = null;
        try {
            fallbkBean = fallback.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        for (Method declaredMethod : fallback.getDeclaredMethods()) {
            String key = getInvokerKey(declaredMethod, declaredMethod.getParameterTypes());
            fallbackBeans.put(key,fallbkBean);
            fallbackMethods.put(key,declaredMethod);
        }
        T proxy = getProxy(clazz,waitSec,demotePolicy,retryCnt);
        return proxy;
    }
}
