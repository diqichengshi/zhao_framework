package org.springframework.beans.factory.support;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;

import java.lang.reflect.Method;

public class RootBeanDefinition extends AbstractBeanDefinition {
    boolean allowCaching = true;
    private BeanDefinitionHolder decoratedDefinition;
    private volatile Class<?> targetType;
    boolean isFactoryMethodUnique = false;
    final Object constructorArgumentLock = new Object();
    Object resolvedConstructorOrFactoryMethod;
    Object[] resolvedConstructorArguments;
    Object[] preparedConstructorArguments;
    boolean constructorArgumentsResolved = false;
    final Object postProcessingLock = new Object();
    boolean postProcessed = false;

    public RootBeanDefinition() {
        super();
    }

    public RootBeanDefinition(Class<?> beanClass) {
        super();
        setBeanClass(beanClass);
    }

    RootBeanDefinition(BeanDefinition original) {
        super(original);
    }

    public RootBeanDefinition(RootBeanDefinition original) {
        super(original);
        this.allowCaching = original.allowCaching;
        this.decoratedDefinition = original.decoratedDefinition;
        this.targetType = original.targetType;
        this.isFactoryMethodUnique = original.isFactoryMethodUnique;
    }

    @Override
    public String getParentName() {
        return null;
    }

    @Override
    public void setParentName(String parentName) {
        if (parentName != null) {
            throw new IllegalArgumentException("Root bean cannot be changed into a child bean with parent reference");
        }
    }

    @Override
    public RootBeanDefinition cloneBeanDefinition() {
        return new RootBeanDefinition(this);
    }

    public BeanDefinitionHolder getDecoratedDefinition() {
        return this.decoratedDefinition;
    }

    public boolean isFactoryMethod(Method candidate) {
        return (candidate != null && candidate.getName().equals(getFactoryMethodName()));
    }
}
