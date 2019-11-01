package org.springframework.beans.factory.support;

import org.springframework.beans.config.BeanDefinition;

public class DefaultBeanNameGenerator implements BeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        return BeanDefinitionReaderUtils.generateBeanName(definition, registry);
    }

}
