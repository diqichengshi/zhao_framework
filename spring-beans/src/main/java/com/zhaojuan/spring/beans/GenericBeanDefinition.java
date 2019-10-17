package com.zhaojuan.spring.beans;

import java.util.Objects;

public class GenericBeanDefinition implements BeanDefinition {

    private Class<?> beanClass;

    private String scope = BeanDefinition.SINGLETION;

    private String initMethodName;
    private String[] dependsOn;
    private MutablePropertyValues propertyValues;

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }
    @Override
    public String getScope() {
        return scope;
    }
    @Override
    public boolean isSingleton() {
        return Objects.equals(scope, BeanDefinition.SINGLETION);
    }
    @Override
    public boolean isPrototype() {
        return Objects.equals(scope, BeanDefinition.PROTOTYPE);
    }
    @Override
    public String getInitMethodName() {
        return initMethodName;
    }

    /**
     * 返回此bean所依赖的bean名称
     */
    @Override
    public String[] getDependsOn() {
        return this.dependsOn;
    }
    public MutablePropertyValues getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(MutablePropertyValues propertyValues) {
        this.propertyValues = propertyValues;
    }

}
