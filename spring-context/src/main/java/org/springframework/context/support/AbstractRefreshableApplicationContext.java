package org.springframework.context.support;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.exception.BeanDefinitionStoreException;
import org.springframework.beans.exception.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextException;

import java.io.IOException;

public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {
    private DefaultListableBeanFactory beanFactory;
    private final Object beanFactoryMonitor = new Object();

    public AbstractRefreshableApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
    }

    @Override
    public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
        return this.beanFactory;
    }

    @Override
    protected void refreshBeanFactory() throws BeansException, IllegalStateException {
        if (hasBeanFactory()) {
            destroyBeans();
            closeBeanFactory();
        }
        try {
            DefaultListableBeanFactory beanFactory = createBeanFactory();
            loadBeanDefinitions(beanFactory);
            synchronized (this.beanFactoryMonitor) {
                this.beanFactory = beanFactory;
            }
        }
        catch (IOException ex) {
            throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
        }
    }
    protected DefaultListableBeanFactory createBeanFactory() {
        return new DefaultListableBeanFactory();
    }
    protected final void closeBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            this.beanFactory = null;
        }
    }
    protected final boolean hasBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            return (this.beanFactory != null);
        }
    }



    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        return beanFactory.isTypeMatch(name,typeToMatch);
    }


    /**
     * Load bean definitions into the given bean factory, typically through
     * delegating to one or more bean definition readers.
     * @param beanFactory the bean factory to load bean definitions into
     * @throws BeansException if parsing of the bean definitions failed
     * @throws IOException if loading of bean definition files failed
     * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
     * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
     */
    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
            throws BeansException, IOException;

}
