package com.zhaojuan.spring.beans;

public interface SingletonBeanRegistry {
    Object getSingleton(String beanName);
    boolean containsSingleton(String beanName);
}
