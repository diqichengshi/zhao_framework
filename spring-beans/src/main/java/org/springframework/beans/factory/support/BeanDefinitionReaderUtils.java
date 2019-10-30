package org.springframework.beans.factory.support;

import org.springframework.beans.exception.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;

public class BeanDefinitionReaderUtils {
    /**
     * Register the given bean definition with the given bean factory.
     * @param definitionHolder the bean definition including name and aliases
     * @param registry the bean factory to register with
     * @throws BeanDefinitionStoreException if registration failed
     */
    public static void registerBeanDefinition(
            BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
            throws BeanDefinitionStoreException {

        // Register bean definition under primary name.
        String beanName = definitionHolder.getBeanName();
        registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

        // Register aliases for bean name, if any.
        /*String[] aliases = definitionHolder.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                registry.registerAlias(beanName, alias);
            }
        }*/
    }
}
