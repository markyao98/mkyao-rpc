package com.test;

import MicroRpc.framework.beans.ProtocolTypes;
import MicroRpc.framework.context.SpringDubboApplicationContext;
import MicroRpc.framework.loadbalance.LoadBalance;
import MicroRpc.framework.redis.Registry.core.RedisRegistry;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PreDestroy;

@SpringBootApplication
public class ConsumerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(ConsumerApplication.class, args);
        ConfigurableListableBeanFactory configurableListableBeanFactory = run.getBeanFactory();
        SpringDubboApplicationContext context=
                new SpringDubboApplicationContext(configurableListableBeanFactory, ProtocolTypes.DUBBO, LoadBalance.RANDAM_WEIGHT);
        context.refresh();
    }

    @PreDestroy
    public void destroy(){
        RedisRegistry.clearAll();
    }
}
