package com.zhaojuan.spring.beans;

import java.beans.PropertyDescriptor;

public interface BeanWrapper {
    /**
     * 返回包装对象
     */
    Object getWrappedInstance();

    /**
     * 返回包装对象的CLass
     */
    Class<?> getWrappedClass();

    /**
     * 设置配置属性
     */
    void setPropertyValues(PropertyValues pvs) throws BeansException, NoSuchFieldException;

    PropertyDescriptor getPropertyDescriptor(String propertyName) throws BeanCreationException;

}
