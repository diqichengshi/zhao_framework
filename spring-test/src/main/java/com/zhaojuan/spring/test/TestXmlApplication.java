package com.zhaojuan.spring.test;

import com.zhaojuan.spring.context.support.AnnotationApplicationContext;
import com.zhaojuan.spring.context.support.ClassPathXmlApplicationContext;
import com.zhaojuan.spring.test.entity.Role;
import com.zhaojuan.spring.test.entity.User;
import com.zhaojuan.spring.test.service.ProductService;

public class TestXmlApplication {
    public static void main(String[] args) throws Exception {
        //创建ClassPathXmlApplicationContext对象
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("user.xml");
        //使用手动强转的方式获取单例的User对象
        User user = (User) ctx.getBean("user");
        System.out.println("单例user:" + user);
        //使用手动强转的方式获取多例的User对象
        /*Role role = (Role) ctx.getBean("role");
        System.out.println("多例role" + role);*/

        // 获取以来注入的对象
        Role role=user.getRole();
        System.out.println("注入的多例role" + role);
    }

}
