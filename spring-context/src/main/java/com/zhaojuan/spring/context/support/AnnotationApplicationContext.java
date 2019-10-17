package com.zhaojuan.spring.context.support;

import com.zhaojuan.spring.beans.AnnotationBeanFactory;
import com.zhaojuan.spring.context.ApplicationContext;

public class AnnotationApplicationContext extends AnnotationBeanFactory implements ApplicationContext {

    public AnnotationApplicationContext(String packageName) throws Exception {
        super(packageName);
    }
}
