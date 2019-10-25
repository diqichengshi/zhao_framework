package org.springframework.beans.exception;

import org.springframework.beans.factory.FactoryBean;

public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException {

    /**
     * Create a new BeanIsNotAFactoryException.
     * @param name the name of the bean requested
     * @param actualType the actual type returned, which did not match
     * the expected type
     */
    public BeanIsNotAFactoryException(String name, Class<?> actualType) {
        super(name, FactoryBean.class, actualType);
    }

}

