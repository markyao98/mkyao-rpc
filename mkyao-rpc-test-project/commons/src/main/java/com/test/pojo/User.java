package com.test.pojo;


import java.io.Serializable;
import java.util.List;

/**
 * 测试pojo,记住pojo,dto这些类都要实现Serializable接口,因为底层需要用到jdk的序列化
 */
public class User implements Serializable {
    private int id;
    private String name;
    private int age;
    private List<Book>personalBooks;

    public List<Book> getPersonalBooks() {
        return personalBooks;
    }

    public void setPersonalBooks(List<Book> personalBooks) {
        this.personalBooks = personalBooks;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
