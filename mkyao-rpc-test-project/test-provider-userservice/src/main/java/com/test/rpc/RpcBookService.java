package com.test.rpc;

import MicroRpc.framework.commons.ServiceProvider;
import com.test.pojo.Book;
import com.test.utils.TestDataGenarator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 实现公共服务接口 UserService
 * 加上以下两个注解 @ServiceProvider,@Component
 */
@ServiceProvider
@Component
public class RpcBookService implements BookService{

    @Override
    public List<Book> getBooks() {
        return TestDataGenarator.getBooks();
    }

    @Override
    public Book getBook(Integer id) {
        return TestDataGenarator.getBook(id);
    }

    /**
     * 测试服务出错
     * @param msg
     * @return
     */
    @Override
    public String testError(String msg) {
        System.out.println("bookservice 消息:"+msg);
        int i=10/0;
        return "i=10/0";
    }
}
