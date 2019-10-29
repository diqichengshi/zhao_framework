package org.springframework.beans.factory.support;

import org.springframework.beans.config.BeanPostProcessor;

public interface MergedBeanDefinitionPostProcessor  extends BeanPostProcessor {
    /**
     * Post-process the given merged bean definition for the specified bean.
     * @param beanDefinition the merged bean definition for the bean
     * @param beanType the actual type of the managed bean instance
     * @param beanName the name of the bean
     */
    void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);
}
