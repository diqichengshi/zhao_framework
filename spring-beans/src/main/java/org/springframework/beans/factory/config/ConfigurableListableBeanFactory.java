package org.springframework.beans.factory.config;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.exception.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * 大多数可列出bean工厂要实现的配置接口
 */
public interface ConfigurableListableBeanFactory extends ListableBeanFactory {
    /**
     * Return the registered BeanDefinition for the specified bean, allowing access
     * to its property values and constructor argument value (which can be
     * modified during bean factory post-processing).
     * <p>A returned BeanDefinition object should not be a copy but the original
     * definition object as registered in the factory. This means that it should
     * be castable to a more specific implementation type, if necessary.
     * <p><b>NOTE:</b> This method does <i>not</i> consider ancestor factories.
     * It is only meant for accessing local bean definitions of this factory.
     *
     * @param beanName the name of the bean
     * @return the registered BeanDefinition
     * @throws NoSuchBeanDefinitionException if there is no bean with the given name
     *                                       defined in this factory
     */
    BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;
}
