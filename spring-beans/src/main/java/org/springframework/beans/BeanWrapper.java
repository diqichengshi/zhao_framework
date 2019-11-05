package org.springframework.beans;

import org.springframework.beans.exception.InvalidPropertyException;

import java.beans.PropertyDescriptor;

public interface BeanWrapper extends PropertyAccessor, TypeConverter {
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
     *
     * @return the PropertyDescriptors for the wrapped object
     */
    PropertyDescriptor[] getPropertyDescriptors();

    /**
     * Obtain the property descriptor for a specific property
     * of the wrapped object.
     *
     * @param propertyName the property to obtain the descriptor for
     *                     (may be a nested path, but no indexed/mapped property)
     * @return the property descriptor for the specified property
     * @throws InvalidPropertyException if there is no such property
     */
    PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
