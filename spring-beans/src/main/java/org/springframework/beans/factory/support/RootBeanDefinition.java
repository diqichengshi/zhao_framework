package org.springframework.beans.factory.support;

public class RootBeanDefinition extends AbstractBeanDefinition {
    final Object postProcessingLock = new Object();
    boolean postProcessed = false;

}
