package org.springframework.beans.factory.support;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.exception.BeanDefinitionStoreException;

public interface BeanDefinitionRegistry {

    void registerBeanDefinition(String beanName, BeanDefinition hkBeanDefinition) throws BeanDefinitionStoreException;

    BeanDefinition getBeanDefinition(String beanName);

    boolean containsBeanDefinition(String beanName);

    int getBeanDefinitionCount();
}
