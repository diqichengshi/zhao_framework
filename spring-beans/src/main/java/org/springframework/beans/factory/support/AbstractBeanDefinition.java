package org.springframework.beans.factory.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.config.AutowireCapableBeanFactory;
import org.springframework.beans.config.BeanDefinition;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractBeanDefinition implements BeanDefinition {
    public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

    public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

    public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

    public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

    @Deprecated
    public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

    private String beanName;

    private Class<?> beanClass;

    private String scope = BeanDefinition.SINGLETION;

    private String initMethodName;

    private String[] dependsOn;

    private MutablePropertyValues propertyValues;

    private int autowireMode = AUTOWIRE_NO;

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
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

    @Override
    public void setDependsOn(List<String> refList) {
        String[] temp = new String[refList.size()];
        refList.toArray(temp);
        this.dependsOn = temp;
    }

    public MutablePropertyValues getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(MutablePropertyValues propertyValues) {
        this.propertyValues = propertyValues;
    }

    public void setAutowireMode(int autowireMode) {
        this.autowireMode = autowireMode;
    }


    /**
     * 获取自动装配模式,简单起见,只实现AutowireByName
     */
    public int getResolvedAutowireMode() {
        /*if (this.autowireMode == AUTOWIRE_AUTODETECT) {
            // Work out whether to apply setter autowiring or constructor autowiring.
            // If it has a no-arg constructor it's deemed to be setter autowiring,
            // otherwise we'll try constructor autowiring.
            Constructor<?>[] constructors = getBeanClass().getConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterTypes().length == 0) {
                    return AUTOWIRE_BY_TYPE;
                }
            }
            return AUTOWIRE_CONSTRUCTOR;
        }
        else {
            return this.autowireMode;
        }*/
        return AUTOWIRE_BY_NAME;
    }


}
