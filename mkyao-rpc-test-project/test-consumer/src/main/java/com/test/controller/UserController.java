package com.test.controller;

import MicroRpc.framework.commons.DemotePolicy;
import MicroRpc.framework.commons.ServiceRefrence;
import com.test.pojo.RestData;
import com.test.pojo.User;
import com.test.rpc.UserService;
import com.test.rpcFallback.UserServiceFallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class UserController {
    /**
     * 直接在这里引入userservice即可调用服务，使用ServiceRefrence注解
     * 服务出错策略: 1.重试 2.服务降级，执行fallback方法
     * 这里演示服务降级,需要指定降级fallback的class类
     */
    @ServiceRefrence(demotePolicy = DemotePolicy.DEMOTE_FALLBACK,fallback = UserServiceFallback.class)
    UserService rpcUserService;

    @GetMapping("/users/list")
    public RestData userList(){
        List<User> users = rpcUserService.getUsers();
        return RestData.success(users);
    }
    @GetMapping("/user/{id}")
    public RestData getUser(@PathVariable("id") Integer id){
        User user = rpcUserService.getUser(id);
        return RestData.success(user);
    }
    @GetMapping("/user/testError/{msg}")
    public RestData testError(@PathVariable("msg") String msg){
        String s = rpcUserService.testError(msg);
        return RestData.success(s);
    }

}
