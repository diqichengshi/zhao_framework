package com.zhaojuan.spring.beans;

public interface BeanDefinition {
    final static String SINGLETION = "singleton";

    final static String PROTOTYPE = "prototype";

    Class<?> getBeanClass();

    String getScope();

    boolean isSingleton();

    boolean isPrototype();

    String getInitMethodName();
}
