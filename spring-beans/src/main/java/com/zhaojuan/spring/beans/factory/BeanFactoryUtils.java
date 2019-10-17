package com.zhaojuan.spring.beans.factory;

import com.zhaojuan.spring.beans.BeanFactory;

public class BeanFactoryUtils {
    public static boolean isFactoryDereference(String name) {
        return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
    }
}
