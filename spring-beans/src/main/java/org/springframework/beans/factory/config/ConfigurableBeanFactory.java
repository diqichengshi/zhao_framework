package org.springframework.beans.factory.config;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;

public interface ConfigurableBeanFactory extends BeanFactory {

    TypeConverter getTypeConverter();

    boolean isCurrentlyInCreation(String beanName);

}
