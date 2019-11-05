package org.springframework.beans.factory.xml;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

public class XmlBeanFactory extends DefaultListableBeanFactory implements BeanDefinitionRegistry {

    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

    /**
     * Create a new XmlBeanFactory with the given resource,
     * which must be parsable using DOM.
     *
     * @param resource XML resource to load bean definitions from
     * @throws BeansException in case of loading or parsing errors
     */
    public XmlBeanFactory(Resource resource) throws BeansException {
        this(resource, null);
    }

    /**
     * Create a new XmlBeanFactory with the given input stream,
     * which must be parsable using DOM.
     *
     * @param resource          XML resource to load bean definitions from
     * @param parentBeanFactory parent bean factory
     * @throws BeansException in case of loading or parsing errors
     */
    public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
        super(parentBeanFactory);
        this.reader.loadBeanDefinitions(resource);
    }
}