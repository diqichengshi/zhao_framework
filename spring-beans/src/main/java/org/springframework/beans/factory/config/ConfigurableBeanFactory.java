package org.springframework.beans.factory.config;

import org.springframework.beans.factory.BeanFactory;

public interface ConfigurableBeanFactory extends BeanFactory {

    boolean isCurrentlyInCreation(String beanName);

}
