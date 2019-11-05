package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

public interface DestructionAwareBeanPostProcessor  extends BeanPostProcessor {

    /**
     * Apply this BeanPostProcessor to the given bean instance before
     * its destruction. Can invoke custom destruction callbacks.
     * <p>Like DisposableBean's {@code destroy} and a custom destroy method,
     * this callback just applies to singleton beans in the factory (including
     * inner beans).
     * @param bean the bean instance to be destroyed
     * @param beanName the name of the bean
     * @throws org.springframework.beans.BeansException in case of errors
     * @see org.springframework.beans.factory.DisposableBean
     * @see org.springframework.beans.factory.support.AbstractBeanDefinition#setDestroyMethodName
     */
    void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

}
