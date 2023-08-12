package MicroRpc.framework.protocol;

import MicroRpc.framework.beans.Invoker;
import MicroRpc.framework.beans.Url;
import MicroRpc.framework.commons.DemotePolicy;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.Method;

public interface Protocol {

     Object send(Invoker invoker, int waitSec, DemotePolicy demotePolicy, Object fallbk, Method fallbkMethod,int retryCnt);



     void recv(Url url, ConfigurableListableBeanFactory beanFactory);


     void recv(Url url);

     <T> T getService(String interfaceName,int waitSec);

     <T> T getService(Class clazz,int waitSec);

     <T> T getService(Class clazz, int waitSec, DemotePolicy demotePolicy,Class fallback,int retryCnt);


     void init();
}
