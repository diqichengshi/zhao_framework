package org.springframework.beans;

import java.beans.PropertyDescriptor;

public interface BeanWrapper extends PropertyAccessor {
    /**
     * 返回包装对象
     */
    Object getWrappedInstance();

    /**
     * 返回包装对象的CLass
     */
    Class<?> getWrappedClass();
    /**
     * Obtain the PropertyDescriptors for the wrapped object
     * (as determined by standard JavaBeans introspection).
     * @return the PropertyDescriptors for the wrapped object
     */
    PropertyDescriptor[] getPropertyDescriptors();
}
