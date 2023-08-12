package com.test.rpc;

import com.test.pojo.User;

import java.util.List;

/**
 * 测试接口
 */
public interface UserService {

    List<User>getUsers();

    User getUser(Integer id);

    String testError(String msg);

}
