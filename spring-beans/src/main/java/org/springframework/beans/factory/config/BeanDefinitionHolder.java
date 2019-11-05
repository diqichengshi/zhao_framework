package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.util.Assert;

public class BeanDefinitionHolder implements BeanMetadataElement {
    private final BeanDefinition beanDefinition;
    private final String beanName;

    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
        Assert.notNull(beanName, "Bean name must not be null");
        this.beanDefinition = beanDefinition;
        this.beanName = beanName;
    }

    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, String[] aliases) {
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
        Assert.notNull(beanName, "Bean name must not be null");
        this.beanDefinition = beanDefinition;
        this.beanName = beanName;
        // this.aliases = aliases;
    }

    public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
        Assert.notNull(beanDefinitionHolder, "BeanDefinitionHolder must not be null");
        this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
        this.beanName = beanDefinitionHolder.getBeanName();
        // this.aliases = beanDefinitionHolder.getAliases();
    }

    @Override
    public Object getSource() {
        return this.beanDefinition.getSource();
    }

    public String getBeanName() {
        return beanName;
    }

    public BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }
}
