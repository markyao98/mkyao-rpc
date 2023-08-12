package com.test.rpc;

import MicroRpc.framework.commons.ServiceProvider;
import com.test.pojo.User;
import com.test.utils.TestDataGenarator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 实现公共服务接口 UserService
 * 加上以下两个注解 @ServiceProvider,@Component
 */
@ServiceProvider
@Component
public class RpcUserService implements UserService{
    @Override
    public List<User> getUsers() {
        return TestDataGenarator.genaratorUsers();
    }

    @Override
    public User getUser(Integer id) {
        return TestDataGenarator.getUser(id);
    }

    @Override
    public String testError(String msg) {
        System.out.println("userservice 消息:"+msg);
        int i=10/0;
        return "i=10/0";
    }
}
