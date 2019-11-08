package org.springframework.tests.aop;

import org.springframework.stereotype.Component;

@Component
public class Student implements Person{
    public void say(){
        System.out.println("这是一个苦逼的程序员");
    }
}
