package org.springframework.beans.factory.support;

public class RootBeanDefinition extends AbstractBeanDefinition {
    final Object postProcessingLock = new Object();
    boolean postProcessed = false;
    public RootBeanDefinition() {
        super();
    }
    public RootBeanDefinition(Class<?> beanClass) {
        super();
        setBeanClass(beanClass);
    }

}
