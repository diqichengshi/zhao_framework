package org.springframework.aop.aspectj.annotation;

import org.springframework.beans.factory.BeanFactory;

public class PrototypeAspectInstanceFactory  extends BeanFactoryAspectInstanceFactory {

    /**
     * Create a PrototypeAspectInstanceFactory. AspectJ will be called to
     * introspect to create AJType metadata using the type returned for the
     * given bean name from the BeanFactory.
     * @param beanFactory the BeanFactory to obtain instance(s) from
     * @param name the name of the bean
     */
    public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
        super(beanFactory, name);
        if (!beanFactory.isPrototype(name)) {
            throw new IllegalArgumentException(
                    "Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
        }
    }
}
