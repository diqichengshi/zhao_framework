package com.zhaojuan.spring.context.support;

import com.zhaojuan.spring.beans.DefaultBeanFactory;
import com.zhaojuan.spring.beans.XmlBeanFactory;
import com.zhaojuan.spring.context.ApplicationContext;

public class ClassPathXmlApplicationContext extends XmlBeanFactory implements ApplicationContext {

    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public ClassPathXmlApplicationContext(String xmlPath) {
        super(xmlPath);
    }
}