package org.springframework.beans.config;

public interface SingletonBeanRegistry {
    Object getSingleton(String beanName);

    boolean containsSingleton(String beanName);
}
