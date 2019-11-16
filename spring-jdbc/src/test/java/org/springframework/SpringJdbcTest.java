package org.springframework;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.User;
import org.springframework.tests.UserService;

import java.util.List;

public class SpringJdbcTest {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("ApplicationContext.xml");
        UserService userService= (UserService) context.getBean("userService");
        User user=new User();
        user.setName("zhangsan");
        user.setAge(18);
        // 保存一条记录
        userService.save(user);

        // 查询列表
        List<User> userList=userService.getUser();
        for (User user1: userList) {
            System.out.println(user1.toString());
        }
    }
}
