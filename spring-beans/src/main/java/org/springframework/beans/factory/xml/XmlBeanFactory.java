package org.springframework.beans.factory.xml;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.exception.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.util.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XmlBeanFactory extends DefaultListableBeanFactory implements BeanDefinitionRegistry {

    XmlBeanDefinitionReader reader=new XmlBeanDefinitionReader(this);
    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     *
     * @param xmlPath
     */
    public XmlBeanFactory(String xmlPath) {
        super();
        reader.loadBeanDefinitions(xmlPath); // 解析xml
    }

}