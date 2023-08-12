package com.test.controller;

import MicroRpc.framework.commons.DemotePolicy;
import MicroRpc.framework.commons.ServiceRefrence;
import com.test.pojo.Book;
import com.test.pojo.RestData;
import com.test.rpc.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class BookController {
    /**
     * 直接在这里引入bookservice即可调用服务，使用ServiceRefrence注解
     * 服务出错策略: 1.重试 2.服务降级，执行fallback方法
     * 这里演示重试,默认重试3次，可以指定重试次数
     */
    @ServiceRefrence(demotePolicy = DemotePolicy.RETRY,retrycnt = 3)
    BookService rpcBookService;

    @GetMapping("/books/list")
    public RestData bookList(){
        List<Book> books = rpcBookService.getBooks();
        return RestData.success(books);
    }
    @GetMapping("/book/{id}")
    public RestData book(@PathVariable("id") Integer id){
        Book book = rpcBookService.getBook(id);

        return RestData.success(book);
    }
    @GetMapping("/book/testError/{msg}")
    public RestData testError(@PathVariable("msg") String msg){
        String s = rpcBookService.testError(msg);
        return RestData.success(s);
    }


}
