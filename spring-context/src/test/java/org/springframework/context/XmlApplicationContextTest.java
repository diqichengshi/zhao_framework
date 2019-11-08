package org.springframework.context;

import org.springframework.tests.entity.User;
import org.springframework.tests.entity.Menu;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class XmlApplicationContextTest {
    public static void main(String[] args) throws Exception {
        //创建ClassPathXmlApplicationContext对象
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("ApplicationContext.xml");
        //使用手动强转的方式获取单例的User对象
        User user = (User) ctx.getBean("user");
        System.out.println("单例user:" + user);
        //使用手动强转的方式获取多例的User对象
        /*Role role = (Role) ctx.getBean("role");
        System.out.println("多例role" + role);*/

        // 获取以来注入的对象
        /*Role role = user.getRole();
        System.out.println("注入的多例role" + role);*/

        // 获取以来注入的对象
        Menu menu = user.getMenu();
        menu.setId(1);
        menu.setName("系统管理");
        System.out.println("注解注入的对象menu" + menu);
    }

}
