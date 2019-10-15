package com.zhaojuan.spring.test;

import com.zhaojuan.spring.context.support.ClassPathXmlApplicationContext;
import com.zhaojuan.spring.test.service.UserService;

public class TestApplication {
    public static void main(String[] args) throws Exception {
        String pckName = "com.zhaojuan.spring.test.service";
        ClassPathXmlApplicationContext app = new ClassPathXmlApplicationContext(pckName);
        UserService userService = (UserService) app.getBean("userService");
        userService.add("test");
    }

}
