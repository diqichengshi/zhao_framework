package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class RootBeanDefinition extends AbstractBeanDefinition {
    boolean allowCaching = true;
    private BeanDefinitionHolder decoratedDefinition;
    private volatile Class<?> targetType;
    boolean isFactoryMethodUnique = false;
    final Object constructorArgumentLock = new Object();
    Object resolvedConstructorOrFactoryMethod;
    volatile Class<?> resolvedFactoryMethodReturnType;
    Object[] resolvedConstructorArguments;
    Object[] preparedConstructorArguments;
    boolean constructorArgumentsResolved = false;
    final Object postProcessingLock = new Object();
    boolean postProcessed = false;
    volatile Boolean beforeInstantiationResolved;
    private Set<Member> externallyManagedConfigMembers;
    private Set<String> externallyManagedInitMethods;
    private Set<String> externallyManagedDestroyMethods;

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

    public RootBeanDefinition(String beanClassName) {
        setBeanClassName(beanClassName);
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
    /**
     * Specify the target type of this bean definition, if known in advance.
     */
    public void setTargetType(Class<?> targetType) {
        this.targetType = targetType;
    }

    /**
     * Return the target type of this bean definition, if known
     * (either specified in advance or resolved on first instantiation).
     */
    public Class<?> getTargetType() {
        return this.targetType;
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

    public void registerExternallyManagedConfigMember(Member configMember) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedConfigMembers == null) {
                this.externallyManagedConfigMembers = new HashSet<Member>(1);
            }
            this.externallyManagedConfigMembers.add(configMember);
        }
    }

    public boolean isExternallyManagedConfigMember(Member configMember) {
        synchronized (this.postProcessingLock) {
            return (this.externallyManagedConfigMembers != null &&
                    this.externallyManagedConfigMembers.contains(configMember));
        }
    }

    public void registerExternallyManagedInitMethod(String initMethod) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedInitMethods == null) {
                this.externallyManagedInitMethods = new HashSet<String>(1);
            }
            this.externallyManagedInitMethods.add(initMethod);
        }
    }

    public Method getResolvedFactoryMethod() {
        synchronized (this.constructorArgumentLock) {
            Object candidate = this.resolvedConstructorOrFactoryMethod;
            return (candidate instanceof Method ? (Method) candidate : null);
        }
    }

    public boolean isExternallyManagedDestroyMethod(String destroyMethod) {
        synchronized (this.postProcessingLock) {
            return (this.externallyManagedDestroyMethods != null &&
                    this.externallyManagedDestroyMethods.contains(destroyMethod));
        }
    }

}
