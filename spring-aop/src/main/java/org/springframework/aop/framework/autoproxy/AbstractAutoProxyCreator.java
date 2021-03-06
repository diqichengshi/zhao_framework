package org.springframework.aop.framework.autoproxy;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
        implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

    protected static final Object[] DO_NOT_PROXY = null;
    /**
     * Default is global AdvisorAdapterRegistry
     */
    private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
    protected final Log logger = LogFactory.getLog(getClass());
    private boolean freezeProxy = false;
    private String[] interceptorNames = new String[0];
    private boolean applyCommonInterceptorsFirst = true;
    private TargetSourceCreator[] customTargetSourceCreators;
    private BeanFactory beanFactory;
    private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<Object, Boolean>(64);
    private final Set<String> targetSourcedBeans =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));
    private final Set<Object> earlyProxyReferences =
            Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>(16));
    private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<Object, Class<?>>(16);

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
        Object cacheKey = getCacheKey(beanClass, beanName);
        return this.proxyTypes.get(cacheKey);
    }

    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        if (!this.earlyProxyReferences.contains(cacheKey)) {
            this.earlyProxyReferences.add(cacheKey);
        }
        return wrapIfNecessary(bean, beanName, cacheKey);
    }


    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        Object cacheKey = getCacheKey(beanClass, beanName);

        if (beanName == null || !this.targetSourcedBeans.contains(beanName)) {
            if (this.advisedBeans.containsKey(cacheKey)) {
                return null;
            }
            if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return null;
            }
        }

        // Create proxy here if we have a custom TargetSource.
        // Suppresses unnecessary default instantiation of the target bean:
        // The TargetSource will handle target instances in a custom fashion.
        if (beanName != null) {
            TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
            if (targetSource != null) {
                this.targetSourcedBeans.add(beanName);
                Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
                Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
                this.proxyTypes.put(cacheKey, proxy.getClass());
                return proxy;
            }
        }

        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }


    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

        return pvs;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 主要看这个方法,在bean初始化之后对生产出的bean进行包装,包装成proxy对象
     * Create a proxy with the configured interceptors if the bean is
     * identified as one to proxy by the subclass.
     *
     * @see #getAdvicesAndAdvisorsForBean
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean != null) {
            // 根据指定的bean的class和name构建出一个key,格式:beanClassName_beanName
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                // TODO wrapIfNecessary()在bean初始化之后对生产出的bean进行包装(此类实现了BeanPastProcessor)
                Object wrapBean=wrapIfNecessary(bean, beanName, cacheKey);
                /*logger.info("AbstractAutoProxyCreator.postProcessAfterInitialization() 在bean初始化之后对生产出的"
                        + beanName + "进行包装,包装结果:" + wrapBean);*/
                return wrapBean; // 如果它适合被代理,则需要封装指定的bean
            }
        }
        return bean;
    }


    protected Object getCacheKey(Class<?> beanClass, String beanName) {
        return beanClass.getName() + "_" + beanName;
    }

    /**
     * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
     * 在bean初始化之后对生产出的bean进行包装
     *
     * @param bean     the raw bean instance
     * @param beanName the name of the bean
     * @param cacheKey the cache key for metadata access
     * @return a proxy wrapping the bean, or the raw bean instance as-is
     */
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // 1.如果已经处理过或者不需要创建代理,则返回
        if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        }
        // 无需增强
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        }
        // 给定的bean类是否代表一个基础设施类,基础设施类不应代理,或者配置了指定bean不需要自动代理
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }

        // 2.创建代理
        // TODO getAdvicesAndAdvisorsForBean()获取bean匹配的增强拦截器
        // 2.1 根据指定的bean获取所有的适合该bean的增强
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        // 如果获取到了增强方法则需要针对增强创建代理
        if (specificInterceptors != DO_NOT_PROXY) {
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            // TODO 创建代理,把bean包装为proxy的主要方法
            // 2.2 为指定bean创建代理
            Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
            this.proxyTypes.put(cacheKey, proxy.getClass());
            return proxy;
        }

        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }

    /**
     * Return whether the given bean class represents an infrastructure class
     * that should never be proxied.
     * <p>The default implementation considers Advices, Advisors and
     * AopInfrastructureBeans as infrastructure classes.
     *
     * @param beanClass the class of the bean
     * @return whether the bean represents an infrastructure class
     * @see org.aopalliance.aop.Advice
     * @see org.springframework.aop.Advisor
     * @see org.springframework.aop.framework.AopInfrastructureBean
     * @see #shouldSkip
     */
    protected boolean isInfrastructureClass(Class<?> beanClass) {
        boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
                Advisor.class.isAssignableFrom(beanClass) ||
                AopInfrastructureBean.class.isAssignableFrom(beanClass);
        if (retVal && logger.isTraceEnabled()) {
            logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
        }
        return retVal;
    }

    /**
     * Subclasses should override this method to return {@code true} if the
     * given bean should not be considered for auto-proxying by this post-processor.
     * <p>Sometimes we need to be able to avoid this happening if it will lead to
     * a circular reference. This implementation returns {@code false}.
     *
     * @param beanClass the class of the bean
     * @param beanName  the name of the bean
     * @return whether to skip the given bean
     */
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        return false;
    }
    /**
     * Create a target source for bean instances. Uses any TargetSourceCreators if set.
     * Returns {@code null} if no custom TargetSource should be used.
     * <p>This implementation uses the "customTargetSourceCreators" property.
     * Subclasses can override this method to use a different mechanism.
     * @param beanClass the class of the bean to create a TargetSource for
     * @param beanName the name of the bean
     * @return a TargetSource for this bean
     * @see #setCustomTargetSourceCreators
     */
    protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
        // We can't create fancy target sources for directly registered singletons.
        if (this.customTargetSourceCreators != null &&
                this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
            for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
                TargetSource ts = tsc.getTargetSource(beanClass, beanName);
                if (ts != null) {
                    // Found a matching TargetSource.
                    if (logger.isDebugEnabled()) {
                        logger.debug("TargetSourceCreator [" + tsc +
                                " found custom TargetSource for bean with name '" + beanName + "'");
                    }
                    return ts;
                }
            }
        }

        // No custom TargetSource found.
        return null;
    }

    /**
     * Create an AOP proxy for the given bean.
     * 把bean包装为proxy的主要方法
     *
     * @param beanClass            the class of the bean
     * @param beanName             the name of the bean
     * @param specificInterceptors the set of interceptors that is
     *                             specific to this bean (may be empty, but not null)
     * @param targetSource         the TargetSource for the proxy,
     *                             already pre-configured to access the bean
     * @return the AOP proxy for the bean
     * @see #buildAdvisors
     */
    protected Object createProxy(
            Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {
        // 创建proxyFactory,代理的生产主要就是在proxyFactory做的
        ProxyFactory proxyFactory = new ProxyFactory();
        // 获取相关类中相关属性
        proxyFactory.copyFrom(this);

        // 决定对于给定的bean是否应该使用targetClass而不是它的接口代理
        // 检查proxyTargetClass设置以及preserveTargetClass属性
        if (!proxyFactory.isProxyTargetClass()) {
            if (shouldProxyTargetClass(beanClass, beanName)) {
                proxyFactory.setProxyTargetClass(true);
            } else {
                evaluateProxyInterfaces(beanClass, proxyFactory);
            }
        }

        // TODO 将拦截器封装为增强器
        Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
        for (Advisor advisor : advisors) {
            // 加入增强器
            proxyFactory.addAdvisor(advisor);
        }

        // 设置要代理的类
        proxyFactory.setTargetSource(targetSource);
        // 定制代理
        customizeProxyFactory(proxyFactory);

        proxyFactory.setFrozen(this.freezeProxy);
        if (advisorsPreFiltered()) {
            proxyFactory.setPreFiltered(true);
        }

        // TODO 把对代理类的创建和处理委托给ProxyFactory去处理
        return proxyFactory.getProxy(getProxyClassLoader());
    }

    /**
     * Determine whether the given bean should be proxied with its target class rather than its interfaces.
     * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
     * of the corresponding bean definition.
     *
     * @param beanClass the class of the bean
     * @param beanName  the name of the bean
     * @return whether the given bean should be proxied with its target class
     * @see AutoProxyUtils#shouldProxyTargetClass
     */
    protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
        return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
                AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
    }

    /**
     * Return whether the Advisors returned by the subclass are pre-filtered
     * to match the bean's target class already, allowing the ClassFilter check
     * to be skipped when building advisors chains for AOP invocations.
     * <p>Default is {@code false}. Subclasses may override this if they
     * will always return pre-filtered Advisors.
     *
     * @return whether the Advisors are pre-filtered
     * @see #getAdvicesAndAdvisorsForBean
     * @see org.springframework.aop.framework.Advised#setPreFiltered
     */
    protected boolean advisorsPreFiltered() {
        return false;
    }

    /**
     * Determine the advisors for the given bean, including the specific interceptors
     * as well as the common interceptor, all adapted to the Advisor interface.
     *
     * @param beanName             the name of the bean
     * @param specificInterceptors the set of interceptors that is
     *                             specific to this bean (may be empty, but not null)
     * @return the list of Advisors for the given bean
     */
    protected Advisor[] buildAdvisors(String beanName, Object[] specificInterceptors) {
        // Handle prototypes correctly...
        // 解析注册的所有InterceptorName
        Advisor[] commonInterceptors = resolveInterceptorNames();

        List<Object> allInterceptors = new ArrayList<Object>();
        if (specificInterceptors != null) {
            // 加入拦截器
            allInterceptors.addAll(Arrays.asList(specificInterceptors));
            if (commonInterceptors != null) {
                if (this.applyCommonInterceptorsFirst) {
                    allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
                } else {
                    allInterceptors.addAll(Arrays.asList(commonInterceptors));
                }
            }
        }
        if (logger.isDebugEnabled()) {
            int nrOfCommonInterceptors = (commonInterceptors != null ? commonInterceptors.length : 0);
            int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
            logger.debug("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
                    " common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
        }


        Advisor[] advisors = new Advisor[allInterceptors.size()];
        for (int i = 0; i < allInterceptors.size(); i++) {
            // 拦截器封装转化为Advisor
            advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
        }
        return advisors;
    }

    /**
     * Resolves the specified interceptor names to Advisor objects.
     *
     * @see #setInterceptorNames
     */
    private Advisor[] resolveInterceptorNames() {
        ConfigurableBeanFactory cbf = (this.beanFactory instanceof ConfigurableBeanFactory) ?
                (ConfigurableBeanFactory) this.beanFactory : null;
        List<Advisor> advisors = new ArrayList<Advisor>();
        for (String beanName : this.interceptorNames) {
            if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
                Object next = this.beanFactory.getBean(beanName);
                advisors.add(this.advisorAdapterRegistry.wrap(next));
            }
        }
        return advisors.toArray(new Advisor[advisors.size()]);
    }

    /**
     * Subclasses may choose to implement this: for example,
     * to change the interfaces exposed.
     * <p>The default implementation is empty.
     *
     * @param proxyFactory ProxyFactory that is already configured with
     *                     TargetSource and interfaces and will be used to create the proxy
     *                     immediably after this method returns
     */
    protected void customizeProxyFactory(ProxyFactory proxyFactory) {
    }

    /**
     * 如果存在增强方法则创建代理(意思就是如果该类有advice则创建proxy)
     * Return whether the given bean is to be proxied, what additional
     * advices (e.g. AOP Alliance interceptors) and advisors to apply.
     *
     * @param beanClass          the class of the bean to advise
     * @param beanName           the name of the bean
     * @param customTargetSource the TargetSource returned by the
     *                           {@link #getCustomTargetSource} method: may be ignored.
     *                           Will be {@code null} if no custom target source is in use.
     * @return an array of additional interceptors for the particular bean;
     * or an empty array if no additional interceptors but just the common ones;
     * or {@code null} if no proxy at all, not even with the common interceptors.
     * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
     * @throws BeansException in case of errors
     * @see #DO_NOT_PROXY
     * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
     */
    protected abstract Object[] getAdvicesAndAdvisorsForBean(
            Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException;
}
