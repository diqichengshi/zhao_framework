package org.springframework.context.support;

import org.springframework.beans.XmlBeanFactory;
import org.springframework.context.ApplicationContext;

public class ClassPathXmlApplicationContext extends XmlBeanFactory implements ApplicationContext {

    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public ClassPathXmlApplicationContext(String xmlPath) {
        super(xmlPath);
    }
}