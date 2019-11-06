package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;

import java.io.IOException;

public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {
    private Boolean allowBeanDefinitionOverriding;
    private Boolean allowCircularReferences;
    private DefaultListableBeanFactory beanFactory;
    private final Object beanFactoryMonitor = new Object();

    public AbstractRefreshableApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
    }

    /**
     * Create a new AbstractRefreshableApplicationContext with the given parent context.
     *
     * @param parent the parent context
     */
    public AbstractRefreshableApplicationContext(ApplicationContext parent) {
        super(parent);
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
            // 创建DefaultListableBeanFactory,就是XmlBeanFactory继承的
            DefaultListableBeanFactory beanFactory = createBeanFactory();
            // 为了序列化指定id,如果需要的话,让这个BeanFactory从id反序列化到BeanFactory对象
            beanFactory.setSerializationId(getId());
            // 定制beanFactory,设置相关属性,包括允许覆盖同名称的不同定义的对象和循环依赖
            // 可以被子类重新实现设置DefaultListableBeanFactory的任何配置
            customizeBeanFactory(beanFactory);
            // 初始化DocumentReader，并进行XML文件的读取和解析
            loadBeanDefinitions(beanFactory);
            // 使用全局变量记录BeanFactory类实例
            synchronized (this.beanFactoryMonitor) {
                this.beanFactory = beanFactory;
            }
        } catch (IOException ex) {
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
        return beanFactory.isTypeMatch(name, typeToMatch);
    }

    /**
     * Customize the internal bean factory used by this context.
     * Called for each {@link #refresh()} attempt.
     * <p>The default implementation applies this context's
     * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
     * and {@linkplain #setAllowCircularReferences "allowCircularReferences"} settings,
     * if specified. Can be overridden in subclasses to customize any of
     * {@link DefaultListableBeanFactory}'s settings.
     * @param beanFactory the newly created bean factory for this context
     * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
     * @see DefaultListableBeanFactory#setAllowCircularReferences
     * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
     * @see DefaultListableBeanFactory#setAllowEagerClassLoading
     */
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        if (this.allowBeanDefinitionOverriding != null) {
            beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
        }
        if (this.allowCircularReferences != null) {
            beanFactory.setAllowCircularReferences(this.allowCircularReferences);
        }
    }

    /**
     * Load bean definitions into the given bean factory, typically through
     * delegating to one or more bean definition readers.
     *
     * @param beanFactory the bean factory to load bean definitions into
     * @throws BeansException if parsing of the bean definitions failed
     * @throws IOException    if loading of bean definition files failed
     * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
     * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
     */
    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
            throws BeansException, IOException;

}
