package com.zhaojuan.spring.context.support;

import com.zhaojuan.spring.beans.AnnotationBeanFactory;
import com.zhaojuan.spring.beans.annotation.Autowired;
import com.zhaojuan.spring.beans.annotation.Service;
import com.zhaojuan.spring.context.ApplicationContext;
import com.zhaojuan.spring.core.util.ClassUtil;
import com.zhaojuan.spring.core.util.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationApplicationContext extends AnnotationBeanFactory implements ApplicationContext {

    public AnnotationApplicationContext(String packageName) throws Exception {
        super(packageName);
    }
}
