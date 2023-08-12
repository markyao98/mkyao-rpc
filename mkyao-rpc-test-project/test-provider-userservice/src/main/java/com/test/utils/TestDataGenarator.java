package com.test.utils;



import com.test.pojo.Book;
import com.test.pojo.User;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 生成测试数据
 */
public class TestDataGenarator {
    private static Map<Integer, User>users;
    private static Map<Integer, Book>books;

    private final static String[]bookNames=
            {"Java 编程语言（第三版 )","Java 编程思想（第 2版 )","Java 编程思想（第 3版 )","JAVA2 核心技术卷 I：基础知识","JAVA2 核心技术卷 II：高级特性",
            "Tomcat 与 JavaWeb 开发技术详解","精通 Hibernate ： Java 对象持久化技术详解","EssentialC++ 中文版","精通 Struts: 基于 MVC 的 JavaWeb 设计与开发","EffectiveJava 中文版",
            "JAVA2 核心技术卷 II：高级特性","C# 程序设计","C++ 语言的设计和演化","深度探索 C++ 对象模型"," Essentitalc++",
            "C#Primer 中文版",".NET 程序设计技术内幕","C++ 标准程序库 — 自修教程与参考手册","MoreEffectiveC++ 中文版","C++ 编程思想（第 2版）第 1卷",
            ".NET 框架程序设计 ",".NET 本质论 --第 1卷","C++Primer( 第 4版 )中文版","C++ 程序设计","C++ 编程思想（第 2版）第 2卷",
            "c#Windows 程序设计","C++ 程序设计语言（特别版 )","C++Primer( 第 3版 )中文版","C++ 程序设计教程 (第 2版 )","C++PrimerPlus( 第五版 )中文版 "};
    private static Random random=new Random();

    public static List<User>genaratorUsers(){
        if (users!=null && !users.isEmpty()){
            return users.values().stream().collect(Collectors.toList());
        }
        if (books==null){
            books=genaratorBooks();
        }
        users=new HashMap<>();
        for (int i = 0; i < 10; i++) {
            User user=new User();
            user.setId(i);
            user.setAge(random.nextInt(100));
            int size1 = random.nextInt(books.size());
            int size2 = random.nextInt(books.size());

            List<Book> books = TestDataGenarator.books.values().stream().collect(Collectors.toList());

            List<Book>bookList=new ArrayList<>();
            int start=Math.min(size1,size2);
            int end=Math.max(size1,size2);
            for (int j = start; j < end; j++) {
                bookList.add(books.get(j));
            }

            user.setPersonalBooks(bookList);
            users.put(i,user);
        }
        return users.values().stream().collect(Collectors.toList());
    }

    public static void main(String[] args) {
        genaratorUsers();
    }
    private static Map<Integer, Book> genaratorBooks() {
        if (books!=null && !books.isEmpty()){
            return books;
        }
        books=new HashMap<>();
        for (int i = 0; i < 30; i++) {
            Book book=new Book();
            book.setId(i);
            book.setBookName(bookNames[i]);
            books.put(i,book);
        }
        return books;

    }
    public static List<Book> getBooks(){
        if (books==null){
            genaratorBooks();
        }
        return books.values().stream().collect(Collectors.toList());
    }

    public static Book getBook(int id){
        if (books==null){
            genaratorBooks();
        }
        return books.get(id);
    }

    public static User getUser(int id){
        if (users==null){
            genaratorUsers();
        }
        return users.get(id);
    }

}
