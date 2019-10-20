package com.zhaojuan.spring.beans.config;

import com.zhaojuan.spring.beans.MutablePropertyValues;

import java.util.List;

public interface BeanDefinition {
    final static String SINGLETION = "singleton";

    final static String PROTOTYPE = "prototype";

    Class<?> getBeanClass();

    String getScope();

    boolean isSingleton();

    boolean isPrototype();

    String getInitMethodName();

    public String[] getDependsOn();

    public void setDependsOn(List<String> refList);

    MutablePropertyValues getPropertyValues();

    public int getResolvedAutowireMode();
}
