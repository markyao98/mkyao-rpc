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

/**
 * Springboot启动类，固定写法.此处是为了启动rpc服务,记得开启本地redis9527端口
 */
@SpringBootApplication
public class Provider1Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Provider1Application.class, args);
        ConfigurableListableBeanFactory configurableListableBeanFactory =null;
        while (configurableListableBeanFactory==null){
            sleep(100);
            configurableListableBeanFactory=run.getBeanFactory();
        }
        SpringDubboApplicationContext context=
                new SpringDubboApplicationContext(configurableListableBeanFactory, ProtocolTypes.DUBBO, LoadBalance.RANDAM_WEIGHT);
        context.refresh();
    }

    @PreDestroy
    public void destroy(){
        RedisRegistry.clearAll();
    }
    private static void sleep(int times) {
        try {
            Thread.sleep(times);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
