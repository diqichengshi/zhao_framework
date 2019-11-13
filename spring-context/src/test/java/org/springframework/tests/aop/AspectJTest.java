package org.springframework.tests.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class AspectJTest {
    @Pointcut("execution(* org.springframework.tests.aop.*.say(..))")
    public void test(){}

    @Before("test()")
    public void before(){
        System.out.println("before test..");
    }

    @After("test()")
    public void after(){
        System.out.println("after test..");
    }

    @Around("test()")
    public Object around(ProceedingJoinPoint p){
        System.out.println("before1");
        Object o = null;
        try {
            o = p.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("after1");
        return o;
    }
}
