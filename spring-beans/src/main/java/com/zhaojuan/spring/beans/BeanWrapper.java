package com.zhaojuan.spring.beans;

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
}
