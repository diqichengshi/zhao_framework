package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.util.Assert;

public class BeanDefinitionHolder implements BeanMetadataElement{
    private final BeanDefinition beanDefinition;
    private final String beanName;
    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
        Assert.notNull(beanName, "Bean name must not be null");
        this.beanDefinition = beanDefinition;
        this.beanName = beanName;
    }
    @Override
    public Object getSource() {
        return this.beanDefinition.getBeanClass();
    }

    public String getBeanName() {
        return beanName;
    }

    public BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }
}
