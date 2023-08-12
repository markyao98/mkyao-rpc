package MicroRpc.framework.commons;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static MicroRpc.framework.commons.DemotePolicy.RETRY;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceRefrence {

    //服务等待时长
    int waitSec() default 1;

    //服务降级策略
    DemotePolicy demotePolicy() default RETRY;

    //重试次数
    int retrycnt() default 3;

    //降级回调类
    Class fallback() default DefaultFallback.class;

}
