package com.zhaojuan.spring.beans;

public interface BeanDefinitionRegistry {

    void registerBeanDefinition(String beanName, BeanDefinition hkBeanDefinition) throws Exception;

    BeanDefinition getBeanDefinition(String beanName);

    boolean containsBeanDefinition(String beanName);
}
