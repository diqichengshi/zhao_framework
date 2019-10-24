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
    /**
     * Obtain the PropertyDescriptors for the wrapped object
     * (as determined by standard JavaBeans introspection).
     * @return the PropertyDescriptors for the wrapped object
     */
    PropertyDescriptor[] getPropertyDescriptors();
}
