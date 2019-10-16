package com.zhaojuan.spring.test;

import com.zhaojuan.spring.context.support.ClassPathApplicationContext;
import com.zhaojuan.spring.test.service.UserService;

public class TestApplication {
    public static void main(String[] args) throws Exception {
        String pckName = "com.zhaojuan.spring.test.service";
        ClassPathApplicationContext app = new ClassPathApplicationContext(pckName);
        UserService userService = (UserService) app.getBean("userService");
        userService.add("test");
    }

}
