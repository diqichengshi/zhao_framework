package org.springframework;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.aop.Person;

public class AopApplicationContextTest {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("AopApplicationTest.xml");
        Person bean2 = (Person)ac.getBean("student");
        bean2.say();
    }
}
