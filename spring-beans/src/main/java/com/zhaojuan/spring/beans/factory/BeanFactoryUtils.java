package com.zhaojuan.spring.beans.factory;


public class BeanFactoryUtils {
    public static boolean isFactoryDereference(String name) {
        return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
    }
}
