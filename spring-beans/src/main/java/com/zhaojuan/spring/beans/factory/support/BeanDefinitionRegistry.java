package com.zhaojuan.spring.beans.factory.support;

import com.zhaojuan.spring.beans.config.BeanDefinition;

public interface BeanDefinitionRegistry {

    void registerBeanDefinition(String beanName, BeanDefinition hkBeanDefinition) throws Exception;

    BeanDefinition getBeanDefinition(String beanName);

    boolean containsBeanDefinition(String beanName);
}
