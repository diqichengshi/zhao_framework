package org.springframework.context.support;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.exception.BeanDefinitionStoreException;
import org.springframework.beans.exception.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;

public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {
    private final DefaultListableBeanFactory beanFactory;

    public GenericApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
    }

    public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanFactory.getBeanDefinition(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.beanFactory.containsBeanDefinition(beanName);
    }

    @Override
    public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
        return this.beanFactory;
    }

    @Override
    protected void refreshBeanFactory() throws BeansException, IllegalStateException {
        /*if (!this.refreshed.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
        }
        this.beanFactory.setSerializationId(getId());*/
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        return this.beanFactory.isTypeMatch(name, typeToMatch);
    }
}
