package org.springframework.beans.factory;

import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;

public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException {

    private static final long serialVersionUID = 7048680356187825440L;

    /**
     * Create a new BeanIsNotAFactoryException.
     *
     * @param name       the name of the bean requested
     * @param actualType the actual type returned, which did not match the expected
     *                   type
     */
    public BeanIsNotAFactoryException(String name, Class<?> actualType) {
        super(name, FactoryBean.class, actualType);
    }

}

