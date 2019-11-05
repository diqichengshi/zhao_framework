package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.exception.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
    protected final Log logger = LogFactory.getLog(getClass());
    private ApplicationContext parent;
    private String displayName = ObjectUtils.identityToString(this);
    private ConfigurableEnvironment environment;
    private final Object startupShutdownMonitor = new Object();
    private long startupDate;
    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public AbstractApplicationContext() {
    }

    public AbstractApplicationContext(ApplicationContext parent) {
        this();
        setParent(parent);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    public void setParent(ApplicationContext parent) {
        this.parent = parent;
        if (parent != null) {
            Environment parentEnvironment = parent.getEnvironment();
            if (parentEnvironment instanceof ConfigurableEnvironment) {
                getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
            }
        }
    }

    protected ConfigurableEnvironment createEnvironment() {
        return new StandardEnvironment();
    }

    /**
     * 获取BeanFactory配置清单
     * 此处进行精简
     */
    @Override
    public void refresh() throws BeansException, IllegalStateException {
        synchronized (this.startupShutdownMonitor) {
            // Prepare this context for refreshing.
            prepareRefresh();

            // Tell the subclass to refresh the internal bean factory.
            BeanFactory beanFactory = obtainFreshBeanFactory();

            // Prepare the bean factory for use in this context.
            prepareBeanFactory(beanFactory);

            try {
                // Allows post-processing of the bean factory in context subclasses.
                postProcessBeanFactory(beanFactory);
                // Invoke factory processors registered as beans in the context.
                invokeBeanFactoryPostProcessors(beanFactory);
                // Register bean processors that intercept bean creation.
                registerBeanPostProcessors(beanFactory);
                // Initialize message source for this context.
                initMessageSource();
                // Initialize event multicaster for this context.
                initApplicationEventMulticaster();
                // Initialize other special beans in specific context subclasses.
                onRefresh();
                // Check for listener beans and register them.
                registerListeners();
                // Instantiate all remaining (non-lazy-init) singletons.
                finishBeanFactoryInitialization(beanFactory);
                // Last step: publish corresponding event.
                finishRefresh();
            } catch (BeansException ex) {
                logger.warn("Exception encountered during context initialization - cancelling refresh attempt", ex);
                // Destroy already created singletons to avoid dangling resources.
                destroyBeans();
                // Reset 'active' flag.
                cancelRefresh(ex);
                // Propagate exception to caller.
                throw ex;
            } finally {
                // Reset common introspection caches in Spring's core, since we
                // might not ever need metadata for singleton beans anymore...
                resetCommonCaches();
            }
        }
    }


    /**
     * Prepare this context for refreshing, setting its startup date and
     * active flag as well as performing any initialization of property sources.
     */
    protected void prepareRefresh() {
        this.startupDate = System.currentTimeMillis();
        this.closed.set(false);
        this.active.set(true);

        if (logger.isInfoEnabled()) {
            logger.info("Refreshing " + this);
        }

    }

    /**
     * Tell the subclass to refresh the internal bean factory.
     *
     * @return the fresh BeanFactory instance
     * @see #refreshBeanFactory()
     * @see #getBeanFactory()
     */
    protected BeanFactory obtainFreshBeanFactory() {
        // TODO 刷新工厂,为空测创建,并加载XML,抽象方法由子类实现
        refreshBeanFactory();
        BeanFactory beanFactory = getBeanFactory();
        if (logger.isDebugEnabled()) {
            logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
        }
        return beanFactory;
    }


    protected void prepareBeanFactory(BeanFactory beanFactory) {

    }

    protected void postProcessBeanFactory(BeanFactory beanFactory) {
    }

    protected void invokeBeanFactoryPostProcessors(BeanFactory beanFactory) {
        // PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
    }

    protected void registerBeanPostProcessors(BeanFactory beanFactory) {
        // PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
    }

    protected void initMessageSource() {

    }

    protected void initApplicationEventMulticaster() {

    }

    protected void onRefresh() throws BeansException {
        // For subclasses: do nothing by default.
    }

    protected void registerListeners() {
    }

    protected void finishBeanFactoryInitialization(BeanFactory beanFactory) {
    }

    protected void finishRefresh() {
    }

    protected void destroyBeans() {
        //  getBeanFactory().destroySingletons();
    }

    protected void cancelRefresh(BeansException ex) {
        this.active.set(false);
    }

    protected void resetCommonCaches() {
        ResolvableType.clearCache();
        // CachedIntrospectionResults.clearClassLoader(getClassLoader());
    }

    protected void assertBeanFactoryActive() {
        if (!this.active.get()) {
            if (this.closed.get()) {
                throw new IllegalStateException(getDisplayName() + " has been closed already");
            } else {
                throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
            }
        }
    }

    //---------------------------------------------------------------------
    // 实现BeanFactory接口的方法
    //---------------------------------------------------------------------
    @Override
    public Object getBean(String name) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public boolean containsBean(String name) {
        assertBeanFactoryActive();
        return getBeanFactory().containsBean(name);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        assertBeanFactoryActive();
        return getBeanFactory().isTypeMatch(name, typeToMatch);
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return getBeanFactory().getType(name);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return getBeanFactory().containsBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return getBeanFactory().getBeanDefinitionCount();
    }

    /**
     * 抽象方法由子类实现
     */
    public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

    /**
     * 抽象方法由子类实现
     * TODO BeanFactory的刷新,以及bean的解析
     */
    protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

}
