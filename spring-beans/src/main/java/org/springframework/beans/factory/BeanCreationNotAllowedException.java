package org.springframework.beans.factory;

import org.springframework.beans.factory.BeanCreationException;

public class BeanCreationNotAllowedException extends BeanCreationException {

    private static final long serialVersionUID = -4996136228728712344L;

    /**
     * Create a new BeanCreationNotAllowedException.
     *
     * @param beanName the name of the bean requested
     * @param msg      the detail message
     */
    public BeanCreationNotAllowedException(String beanName, String msg) {
        super(beanName, msg);
    }

}
