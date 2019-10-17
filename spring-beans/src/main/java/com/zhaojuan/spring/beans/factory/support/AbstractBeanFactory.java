package com.zhaojuan.spring.beans.factory.support;

import com.zhaojuan.spring.beans.*;
import com.zhaojuan.spring.beans.config.BeanDefinition;
import com.zhaojuan.spring.beans.config.Scope;
import com.zhaojuan.spring.beans.factory.BeanFactory;
import com.zhaojuan.spring.beans.factory.BeanFactoryUtils;
import com.zhaojuan.spring.beans.factory.FactoryBean;
import com.zhaojuan.spring.beans.factory.ObjectFactory;
import com.zhaojuan.spring.core.util.Assert;
import com.zhaojuan.spring.core.util.ClassUtils;
import com.zhaojuan.spring.core.util.ReflectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements BeanFactory {
    protected final Log logger = LogFactory.getLog(getClass());
    private BeanFactory parentBeanFactory;
    /**
     * Names of beans that are currently in creation
     */
    private final ThreadLocal<Object> prototypesCurrentlyInCreation = new ThreadLocal<Object>();
    /**
     * Names of beans that have already been created at least once
     */
    private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(64));
    /**
     * Map from scope identifier String to corresponding Scope
     */
    private final Map<String, Scope> scopes = new LinkedHashMap<String, Scope>(8);

    /*
     * 获得bean的门面方法
     * 采用构造器来创建对象
     */
    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null, null, false);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return doGetBean(name, null, args, false);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return doGetBean(name, requiredType, null, false);

    }

    private <T> T doGetBean(String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly) throws BeansException {
        String beanName = name;
        Assert.notNull(beanName, "beanName不能为空");
        Object bean;
        // Eagerly check singleton cache for manually registered singletons.
        //1.是否创建过，yes(正常情况在第一次refreash，单线程的情况下走这个case) 直接返回，否走2
        //2.是否正在创建，yes ，在获取一次bean，如果还没有，则获取singletonFactory去createcreate bean
        //3.singletonFactories数据填充流程，后面getBean的时候详细分析，这个地方先认为没有创建
        Object sharedInstance = getSingleton(beanName);
        //如果args不为空，不管有没有缓存，都要重新根据args 创建一个新的bean
        //case:有缓存且不需要根据args重新创建的
        if (sharedInstance != null) {
            if (logger.isDebugEnabled()) {
                if (isSingletonCurrentlyInCreation(beanName)) {
                    logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
                            "' that is not fully initialized yet - a consequence of a circular reference");
                } else {
                    logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
                }
            }
            //1.不同bean实例，直接返回该bean   2.是factoryBean，则返回该生成的bean，单例情况下，缓存该factorybean
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
        } else {
            //必须创建一个新Bean
            // Fail if we're already creating this bean instance:
            // We're assumably within a circular reference.
            //当前线程正在创建Prototype类型bean，抛异常
            if (isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeansException(beanName + "正在创建中");
            }

            // Check if bean definition exists in this factory.
            BeanFactory parentBeanFactory = getParentBeanFactory();
            //当前容器没有,从父容器找
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
                // Not found -> check parent.
                String nameToLookup = name;
                if (args != null) {
                    // Delegation to parent with explicit args.
                    return (T) parentBeanFactory.getBean(nameToLookup, args);
                } else {
                    // No args -> delegate to standard getBean method.
                    return parentBeanFactory.getBean(nameToLookup, requiredType);
                }
            }

            if (!typeCheckOnly) {
                //把beanName 添加到类型为set的alreadyCreated
                markBeanAsCreated(beanName);
            }

            try {
                final BeanDefinition mbd = getBeanDefinition(beanName);
                // 保证当前bean所依赖的bean的初始化
                String[] dependsOn = mbd.getDependsOn();
                if (dependsOn != null) {
                    for (String dependsOnBean : dependsOn) {
                        //检查是否有循环依赖
                        if (isDependent(beanName, dependsOnBean)) {
                            throw new BeanCreationException(beanName + "循环依赖 between '" + beanName + "' and '" + dependsOnBean + "'");
                        }
                        //注册依赖，Map<String, Set<String>>保存，beanName为key，dependsOnBean add 到set中
                        registerDependentBean(dependsOnBean, beanName);
                        //先初始化依赖
                        getBean(dependsOnBean);
                    }
                }

                // Create bean instance.
                //单例，开始创建bean
                if (mbd.isSingleton()) {
                    //getSingleton方法比较简单，不进行分析了，依次做了以下功能：
                    //1.创建前各个状态位校验，正在销毁，不是正在创建状态，抛出异常
                    //2.调用ObjectFactory匿名内部类的的getObject()
                    //3.getObject以后，判断各个状态位是否正常
                    //4.把getObject的bean放放入缓存
                    sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
                        @Override
                        public Object getObject() throws BeansException {
                            try {
                                //哈哈，这里是核心方法createBean，下面跟进分析
                                return createBean(beanName, mbd, args);
                            } catch (BeansException ex) {
                                // 从单例缓存显式删除实例
                                destroySingleton(beanName);
                                throw ex;
                            }
                        }
                    });
                    bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
                }
                //todo
                else if (mbd.isPrototype()) {
                    // It's a prototype -> create a new instance.
                    Object prototypeInstance = null;
                    try {
                        //设置当前线程在create此bean
                        beforePrototypeCreation(beanName);
                        //和单例一个方法，后面会分析到
                        prototypeInstance = createBean(beanName, mbd, args);
                    } finally {
                        afterPrototypeCreation(beanName);  //移除当前创建
                    }
                    bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
                } else {
                    String scopeName = mbd.getScope();
                    //其他scope的用scope.get
                    final Scope scope = this.scopes.get(scopeName);
                    if (scope == null) {
                        throw new IllegalStateException("No Scope registered for scope '" + scopeName + "'");
                    }
                    try {
                        Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
                            @Override
                            public Object getObject() throws BeansException {
                                beforePrototypeCreation(beanName);
                                try {
                                    return createBean(beanName, mbd, args);
                                } finally {
                                    afterPrototypeCreation(beanName);
                                }
                            }
                        });
                        bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                    } catch (IllegalStateException ex) {
                        throw new BeansException(beanName +
                                "Scope '" + scopeName + "' is not active for the current thread; " +
                                "consider defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                                ex);
                    }
                }
            } catch (BeansException ex) {
                cleanupAfterBeanCreationFailure(beanName);
                throw ex;
            }
        }

        return (T) bean;
    }

    /**
     * 实例化bean
     */
    protected BeanWrapper instantiateBean(final String beanName, final BeanDefinition bd) {
        try {
            Object beanInstance;

            beanInstance = bd.getBeanClass().newInstance();
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw);
            return bw;
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new BeansException(beanName + " Instantiation of bean failed", ex);
        }
    }

    /**
     * 初始化BeanWrapper,此处暂不处理
     */
    private void initBeanWrapper(BeanWrapper bw) {
    }

    protected Object initializeBean(final String beanName, final Object bean, BeanDefinition mbd) {

        Object wrappedBean = bean;
        // 前置处理器处理  TODO
        /*if (mbd == null ) {
            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        }*/

        try {
            invokeInitMethods(beanName, wrappedBean, mbd);
        } catch (Throwable ex) {
            throw new BeansException(beanName + "Invocation of init method failed", ex);
        }
        // 后置处理器处理  TODO
        /*if (mbd == null ) {
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        }*/
        return wrappedBean;
    }

    /**
     * 调用初始化方法
     */
    protected void invokeInitMethods(String beanName, final Object bean, BeanDefinition mbd)
            throws Throwable {

        if (mbd != null) {
            String initMethodName = mbd.getInitMethodName();
            if (initMethodName != null && !"afterPropertiesSet".equals(initMethodName)) {
                invokeCustomInitMethod(beanName, bean, mbd);
            }
        }
    }

    /**
     * 调用自定义的初始化方法
     */
    protected void invokeCustomInitMethod(String beanName, final Object bean, BeanDefinition mbd) throws Throwable {
        String initMethodName = mbd.getInitMethodName();
        final Method initMethod = ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName);
        if (initMethod == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No default init method named '" + initMethodName +
                        "' found on bean with name '" + beanName + "'");
            }
            // Ignore non-existent default lifecycle methods.
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
        }

        try {
            ReflectionUtils.makeAccessible(initMethod);
            initMethod.invoke(bean);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    /**
     * Get the object for the given bean instance, either the bean
     * instance itself or its created object in case of a FactoryBean.
     *
     * @param beanInstance the shared bean instance
     * @param name         name that may include factory dereference prefix
     * @param beanName     the canonical bean name
     * @param mbd          the merged bean definition
     * @return the object to expose for the bean
     */
    protected Object getObjectForBeanInstance(
            Object beanInstance, String name, String beanName, BeanDefinition mbd) {

        // Don't let calling code try to dereference the factory if the bean isn't a factory.
        if (BeanFactoryUtils.isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
            throw new BeanCreationException(name + "不是一个FactoryBean");
        }

        // Now we have the bean instance, which may be a normal bean or a FactoryBean.
        // If it's a FactoryBean, we use it to create a bean instance, unless the
        // caller actually wants a reference to the factory.
        if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
            return beanInstance;
        }

        Object object = null;
        if (mbd == null) {
            object = getCachedObjectForFactoryBean(beanName);
        }
        if (object == null) {
            FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
            object = getObjectFromFactoryBean(factory, beanName, false);
        }
        return object;
    }


    protected boolean isPrototypeCurrentlyInCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        return (curVal != null &&
                (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
    }

    /**
     * 把beanName 添加到类型为set的alreadyCreated
     */
    protected void markBeanAsCreated(String beanName) {
        if (!this.alreadyCreated.contains(beanName)) {
            this.alreadyCreated.add(beanName);
        }
    }


    /**
     * Callback before prototype creation.
     * <p>The default implementation register the prototype as currently in creation.
     *
     * @param beanName the name of the prototype about to be created
     * @see #isPrototypeCurrentlyInCreation
     */
    @SuppressWarnings("unchecked")
    protected void beforePrototypeCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        if (curVal == null) {
            this.prototypesCurrentlyInCreation.set(beanName);
        } else if (curVal instanceof String) {
            Set<String> beanNameSet = new HashSet<String>(2);
            beanNameSet.add((String) curVal);
            beanNameSet.add(beanName);
            this.prototypesCurrentlyInCreation.set(beanNameSet);
        } else {
            Set<String> beanNameSet = (Set<String>) curVal;
            beanNameSet.add(beanName);
        }
    }

    /**
     * Callback after prototype creation.
     * <p>The default implementation marks the prototype as not in creation anymore.
     *
     * @param beanName the name of the prototype that has been created
     * @see #isPrototypeCurrentlyInCreation
     */
    @SuppressWarnings("unchecked")
    protected void afterPrototypeCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        if (curVal instanceof String) {
            this.prototypesCurrentlyInCreation.remove();
        } else if (curVal instanceof Set) {
            Set<String> beanNameSet = (Set<String>) curVal;
            beanNameSet.remove(beanName);
            if (beanNameSet.isEmpty()) {
                this.prototypesCurrentlyInCreation.remove();
            }
        }
    }

    /**
     * Perform appropriate cleanup of cached metadata after bean creation failed.
     *
     * @param beanName the name of the bean
     */
    protected void cleanupAfterBeanCreationFailure(String beanName) {
        this.alreadyCreated.remove(beanName);
    }


    public BeanFactory getParentBeanFactory() {
        return this.parentBeanFactory;
    }

    /**
     * 抽象方法宫子类实现
     */
    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    protected abstract boolean containsBeanDefinition(String beanName);

    protected abstract Object createBean(String beanName, BeanDefinition mbd, Object[] args) throws BeanCreationException;
}
