package org.springframework.beans.exception;

public class BeanCreationNotAllowedException extends BeanCreationException {

    /**
     * Create a new BeanCreationNotAllowedException.
     * @param beanName the name of the bean requested
     * @param msg the detail message
     */
    public BeanCreationNotAllowedException(String beanName, String msg) {
        super(beanName, msg);
    }

}
