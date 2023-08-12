package com.test.rpc;

import com.test.pojo.Book;

import java.util.List;

/**
 * 测试接口
 */
public interface BookService {

    List<Book>getBooks();

    Book getBook(Integer id);

    String testError(String msg);
}
