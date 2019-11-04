package org.springframework.beans.factory.support;

import org.springframework.beans.config.BeanDefinition;

public class BeanDefinitionValueResolver {
    private final AbstractBeanFactory beanFactory;

    private final String beanName;

    private final BeanDefinition beanDefinition;

    /**
     * Create a BeanDefinitionValueResolver for the given BeanFactory and BeanDefinition.
     * @param beanFactory the BeanFactory to resolve against
     * @param beanName the name of the bean that we work on
     * @param beanDefinition the BeanDefinition of the bean that we work on
     * @param typeConverter the TypeConverter to use for resolving TypedStringValues
     */
    public BeanDefinitionValueResolver(
            AbstractBeanFactory beanFactory, String beanName, BeanDefinition beanDefinition) {
        this.beanFactory = beanFactory;
        this.beanName = beanName;
        this.beanDefinition = beanDefinition;
    }

    public Object resolveValueIfNecessary(Object argName, Object value) {
        return value;
    }

}
