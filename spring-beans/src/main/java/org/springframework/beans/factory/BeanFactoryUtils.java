package org.springframework.beans.factory;


import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;

public class BeanFactoryUtils {
    public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

    public static boolean isFactoryDereference(String name) {
        return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
    }


}
