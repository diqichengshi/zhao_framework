package org.springframework.beans.factory.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.config.AutowireCapableBeanFactory;
import org.springframework.beans.config.BeanDefinition;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

public class GenericBeanDefinition extends AbstractBeanDefinition implements BeanDefinition {

    private String parentName;

    /**
     * Create a new GenericBeanDefinition, to be configured through its bean
     * properties and configuration methods.
     *
     * @see #setBeanClass
     * @see #setBeanClassName
     * @see #setScope
     * @see #setAutowireMode
     * @see #setDependencyCheck
     * @see #setConstructorArgumentValues
     * @see #setPropertyValues
     */
    public GenericBeanDefinition() {
        super();
    }

    /**
     * Create a new GenericBeanDefinition as deep copy of the given
     * bean definition.
     *
     * @param original the original bean definition to copy from
     */
    public GenericBeanDefinition(BeanDefinition original) {
        super(original);
    }

    @Override
    public AbstractBeanDefinition cloneBeanDefinition() {
        return new GenericBeanDefinition(this);
    }

    @Override
    public String getParentName() {
        return parentName;
    }

    @Override
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
}
