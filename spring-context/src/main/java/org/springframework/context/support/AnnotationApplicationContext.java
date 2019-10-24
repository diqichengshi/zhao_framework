package org.springframework.context.support;

import org.springframework.beans.AnnotationBeanFactory;
import org.springframework.context.ApplicationContext;

public class AnnotationApplicationContext extends AnnotationBeanFactory implements ApplicationContext {

    public AnnotationApplicationContext(String packageName) throws Exception {
        super(packageName);
    }
}
