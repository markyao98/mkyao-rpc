package com.test.rpcFallback;

import com.test.pojo.User;
import com.test.rpc.UserService;

import java.util.List;

/**
 * 服务降级类,需要实现对应服务接口
 */
public class UserServiceFallback implements UserService {
    @Override
    public List<User> getUsers() {
        return null;
    }

    @Override
    public User getUser(Integer id) {
        return null;
    }

    /**
     * 在这里测试报错方法的fallback执行
     * @param msg
     * @return
     */
    @Override
    public String testError(String msg) {
        System.out.println("报错方法的fallback执行");
        return "报错方法的fallback执行,接收到消息: "+msg;
    }
}
