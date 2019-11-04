package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.beans.exception.NoSuchBeanDefinitionException;

public interface BeanFactory {
    String FACTORY_BEAN_PREFIX = "&";

    Object getBean(String name) throws BeansException;

    <T> T getBean(String name, Class<T> requiredType) throws BeansException;

    Object getBean(String name, Object... args) throws BeansException;

    public boolean containsBean(String name);

    public boolean isTypeMatch(String name, Class<?> typeToMatch);

    Class<?> getType(String name) throws NoSuchBeanDefinitionException;
}
