package org.springframework.beans.factory;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;

public class BeanFactoryUtils {
    public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

    public static String transformedBeanName(String name) {
        Assert.notNull(name, "'name' must not be null");
        String beanName = name;
        while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
            beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
        }
        return beanName;
    }

    public static boolean isFactoryDereference(String name) {
        return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
    }


}
