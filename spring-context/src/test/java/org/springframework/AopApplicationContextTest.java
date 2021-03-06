package org.springframework;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.aop.Person;

public class AopApplicationContextTest {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("AopApplicationTest.xml");
        Person bean2 = (Person) context.getBean("student");
        bean2.say();
        context.close();
    }
}
