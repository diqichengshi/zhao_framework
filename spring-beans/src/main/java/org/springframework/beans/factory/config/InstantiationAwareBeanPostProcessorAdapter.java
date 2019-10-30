package org.springframework.beans.factory.config;

import org.springframework.beans.exception.BeansException;

public abstract class InstantiationAwareBeanPostProcessorAdapter implements InstantiationAwareBeanPostProcessor {
    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }
}