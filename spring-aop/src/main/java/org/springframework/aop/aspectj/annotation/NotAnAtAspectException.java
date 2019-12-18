package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.framework.AopConfigException;

public class NotAnAtAspectException extends AopConfigException {

    private static final long serialVersionUID = 3851574318679334416L;
    
    private Class<?> nonAspectClass;


    /**
     * Create a new NotAnAtAspectException for the given class.
     * @param nonAspectClass the offending class
     */
    public NotAnAtAspectException(Class<?> nonAspectClass) {
        super(nonAspectClass.getName() + " is not an @AspectJ aspect");
        this.nonAspectClass = nonAspectClass;
    }

    /**
     * Returns the offending class.
     */
    public Class<?> getNonAspectClass() {
        return this.nonAspectClass;
    }

}

