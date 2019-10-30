package org.springframework.context.support;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.exception.BeanDefinitionStoreException;
import org.springframework.beans.exception.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class ClassPathXmlApplicationContext extends AbstractRefreshableApplicationContext {
    final String configLocations;
    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public ClassPathXmlApplicationContext(String configLocations) {
        this(configLocations, true);
    }

    public ClassPathXmlApplicationContext(String configLocations, boolean refresh)
            throws BeansException {
        super();
        this.configLocations=configLocations;
        if (refresh) {
            refresh();
        }
    }

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        XmlBeanDefinitionReader reader=new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions(configLocations);
    }

    /*@Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        return getBeanFactory().isTypeMatch(name,typeToMatch);
    }*/

}