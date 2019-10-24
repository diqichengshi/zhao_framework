package org.springframework.beans.config;

import org.springframework.beans.MutablePropertyValues;

import java.util.List;

public interface BeanDefinition {
    final static String SINGLETION = "singleton";

    final static String PROTOTYPE = "prototype";

    public String getBeanName();

    public void setBeanName(String beanName);

    Class<?> getBeanClass();

    String getScope();

    boolean isSingleton();

    boolean isPrototype();

    String getInitMethodName();

    public String[] getDependsOn();

    public void setDependsOn(List<String> refList);

    MutablePropertyValues getPropertyValues();

    public void setAutowireMode(int autowireMode);

    public int getResolvedAutowireMode();

}
