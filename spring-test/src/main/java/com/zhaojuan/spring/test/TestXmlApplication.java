package com.zhaojuan.spring.test;

import com.zhaojuan.spring.context.support.AnnotationApplicationContext;
import com.zhaojuan.spring.context.support.ClassPathXmlApplicationContext;
import com.zhaojuan.spring.test.entity.User;
import com.zhaojuan.spring.test.service.ProductService;

public class TestXmlApplication {
    public static void main(String[] args) throws Exception {
        //创建ClassPathXmlApplicationContext对象
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("user.xml");
        //使用手动强转的方式获取单例的User对象
        User user1_1 = (User) ctx.getBean("user1");
        System.out.println("单例user1_1:"+user1_1);
        //使用手动强转的方式获取多例的User对象
        User user2_1 = (User)ctx.getBean("user2");
        System.out.println("多例user2_1:"+user2_1);
    }

}
