package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.exception.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {
    private Resource[] configResources;
    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public ClassPathXmlApplicationContext(String configLocation) {
        this(new String[] {configLocation}, true, null);
    }
    public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
        this(configLocations, true, null);
    }
    public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent)
            throws BeansException {
        super(parent);
        setConfigLocations(configLocations);
        if (refresh) {
            refresh();
        }
    }

    /**
     * Create a new ClassPathXmlApplicationContext with the given parent,
     * loading the definitions from the given XML files and automatically
     * refreshing the context.
     * @param paths array of relative (or absolute) paths within the class path
     * @param clazz the class to load resources with (basis for the given paths)
     * @param parent the parent context
     * @throws BeansException if context creation failed
     * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
     * @see org.springframework.context.support.GenericApplicationContext
     * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
     */
    public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz, ApplicationContext parent)
            throws BeansException {

        super(parent);
        Assert.notNull(paths, "Path array must not be null");
        Assert.notNull(clazz, "Class argument must not be null");
        this.configResources = new Resource[paths.length];
        for (int i = 0; i < paths.length; i++) {
            this.configResources[i] = new ClassPathResource(paths[i], clazz);
        }
        refresh();
    }

}