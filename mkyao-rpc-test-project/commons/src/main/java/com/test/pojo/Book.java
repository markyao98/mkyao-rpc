package com.test.pojo;

import java.io.Serializable;

/**
 * 测试pojo,记住pojo,dto这些类都要实现Serializable接口,因为底层需要用到jdk的序列化
 */
public class Book implements Serializable {
    private int id;
    private String bookName;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
}
