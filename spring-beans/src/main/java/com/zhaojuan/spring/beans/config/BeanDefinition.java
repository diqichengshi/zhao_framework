package com.zhaojuan.spring.beans.config;

import com.zhaojuan.spring.beans.MutablePropertyValues;

public interface BeanDefinition {
    final static String SINGLETION = "singleton";

    final static String PROTOTYPE = "prototype";

    Class<?> getBeanClass();

    String getScope();

    boolean isSingleton();

    boolean isPrototype();

    String getInitMethodName();

    public String[] getDependsOn();

    MutablePropertyValues getPropertyValues();
}
