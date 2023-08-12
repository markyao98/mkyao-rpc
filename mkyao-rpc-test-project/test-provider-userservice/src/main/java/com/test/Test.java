package com.test;

public class Test {
//    private static final String READ_PATH = Provider1Application.class.getClassLoader().getResource("dubbo-application.xml").getPath();

    public static void main(String[] args) {
        String READ_PATH = Test.class.getClassLoader().getResource("dubbo-application.xml").getPath();

        System.out.println(READ_PATH);
    }
}
