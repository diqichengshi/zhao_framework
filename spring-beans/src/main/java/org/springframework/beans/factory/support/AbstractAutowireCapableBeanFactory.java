package org.springframework.beans.factory.support;

import org.springframework.beans.config.AutowireCapableBeanFactory;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.*;
import org.springframework.beans.config.BeanPostProcessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 自动装配以及属性设置
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private BeanFactory parentBeanFactory;
    private final Set<Class<?>> ignoredDependencyTypes = new HashSet<Class<?>>();
    private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<Class<?>>();

    public AbstractAutowireCapableBeanFactory() {
        super();
        /*ignoreDependencyInterface(BeanNameAware.class);
        ignoreDependencyInterface(BeanFactoryAware.class);
        ignoreDependencyInterface(BeanClassLoaderAware.class);*/
    }

    public AbstractAutowireCapableBeanFactory(BeanFactory parentBeanFactory) {
        this();
        setParentBeanFactory(parentBeanFactory);
    }

    public void setParentBeanFactory(BeanFactory parentBeanFactory) {
        if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
            throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
        }
        this.parentBeanFactory = parentBeanFactory;
    }
    protected InstantiationStrategy getInstantiationStrategy() {
        return this.instantiationStrategy;
    }
    protected ParameterNameDiscoverer getParameterNameDiscoverer() {
        return this.parameterNameDiscoverer;
    }
    /**
     * 实现抽象类的方法
     * 这个类的中心方法: 创建一个bean实例，
     * 填充bean实例,应用后处理器等。
     *
     * @see #doCreateBean
     */
    protected Object createBean(final String beanName, final RootBeanDefinition mbd, final Object[] args) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating instance of bean '" + beanName + "'");
        }
        Object beanInstance = doCreateBean(beanName, mbd, args);
        if (logger.isDebugEnabled()) {
            logger.debug("Finished creating instance of bean '" + beanName + "'");
        }
        return beanInstance;
    }

    /**
     * 实际创建指定的bean。已进行预创建处理
     *
     * @param beanName bean的名称
     * @param mbd      合并bean定义
     * @param args     用于构造函数或工厂方法调用的显式参数
     * @返回bean的新实例
     * @如果无法创建bean，则抛出BeanCreationException
     * @see #instantiateBean
     */
    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args) {
        // Instantiate the bean.
        BeanWrapper instanceWrapper = null;
        if (instanceWrapper == null) {
            //这个是主要方法一，生成wrapper下面分析
            // instanceWrapper 是一个被包装过了的 bean，它里面的属性还未赋实际值
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }
        //获取bean和class对象
        final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
        Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);
        // 后置处理器机制,用来处理@AutoWired注解
        // 这一步的作用就是将所有的后置处理器拿出来，并且把名字叫beanName的类中的变量都封装到InjectionMetadata
        // 的injectedElements集合里面，目的是以后从中获取，挨个创建实例，通过反射注入到相应类中
        synchronized (mbd.postProcessingLock) {
            if (!mbd.postProcessed) {
                // 将所有的后置处理器拿出来,并且把名字叫做beanName的类中的变量封装到InjectionMetadata中
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
                mbd.postProcessed = true;
            }
        }

        // 初始化Bean实例
        Object exposedObject = bean;
        try {
            //TODO 属性填充,自动注入,重点方法二
            populateBean(beanName, mbd, instanceWrapper);
            if (exposedObject != null) {
                //实现InitializingBean接口的方法回调，重点方法三
                exposedObject = initializeBean(beanName, exposedObject, mbd); // 执行Bean的初始化方法
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (ex instanceof BeanCreationException) {
                throw (BeanCreationException) ex;
            } else {
                throw new BeanCreationException(beanName + "Initialization of bean failed", ex);
            }
        }
        return exposedObject;
    }


    /**
     * 这一步的作用就是将所有的后置处理器拿出来，并且把名字叫beanName的类中的变量都封装到InjectionMetadata
     * 的injectedElements集合里面，目的是以后从中获取，挨个创建实例，通过反射注入到相应类中
     */
    protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName)
            throws BeansException {

        try {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof MergedBeanDefinitionPostProcessor) {
                    MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
                    bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
                }
            }
        } catch (Exception ex) {
            throw new BeanCreationException(beanName,
                    "Post-processing failed of bean type [" + beanType + "] failed", ex);
        }
    }


    private BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
        // Make sure bean class is actually resolved at this point.
        Class<?> beanClass = resolveBeanClass(mbd, beanName);

        if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
        }

        // Shortcut when re-creating the same bean...
        boolean resolved = false;
        boolean autowireNecessary = false;
        if (args == null) {
            synchronized (mbd.constructorArgumentLock) {
                if (mbd.resolvedConstructorOrFactoryMethod != null) {
                    resolved = true;
                    autowireNecessary = mbd.constructorArgumentsResolved;
                }
            }
        }

        if (resolved) {
            if (autowireNecessary) {
                return autowireConstructor(beanName, mbd, null, null);
            }
            else {
                return instantiateBean(beanName, mbd);
            }
        }

        // Need to determine the constructor...
        Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
        if (ctors != null ||
                mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
                mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
            return autowireConstructor(beanName, mbd, ctors, args);
        }

        // No special handling: simply use no-arg constructor.
        return instantiateBean(beanName, mbd); // 实例化BeanWrpper
    }
    protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(Class<?> beanClass, String beanName)
            throws BeansException {

        if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
                    if (ctors != null) {
                        return ctors;
                    }
                }
            }
        }
        return null;
    }


    /**
     * 实例化BeanWrpper
     */
    protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition bd) {
        try {
            Object beanInstance = bd.getBeanClass().newInstance();
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw); // 初始化BeanWrapper
            return bw;
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new BeanCreationException(beanName + " Instantiation of bean failed", ex);
        }
    }

    protected BeanWrapper autowireConstructor(
            String beanName, RootBeanDefinition mbd, Constructor<?>[] ctors, Object[] explicitArgs) {

        return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
    }

    /**
     * 普通属性填充和自动注入
     * 容器启动为对象赋值的时候,遇到@Autowired注解,会用后置处理器机制,来创建属性的实例,然后再利用反射机制,将实例化好的属性,赋值给对象
     */
    public void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
        PropertyValues pvs = mbd.getPropertyValues();
        if (bw == null) {
            if (!pvs.isEmpty()) {
                throw new BeanCreationException(beanName, "Cannot apply property values to null instance");
            } else {
                // Skip property population phase for null instance.
                return;
            }
        }

        // 后置处理器机制,用来处理@AutoWired注解
        // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
        // state of the bean before properties are set. This can be used, for example,
        // to support styles of field injection.
        boolean continueWithPropertyPopulation = true;

        if (hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                        continueWithPropertyPopulation = false;
                        break;
                    }
                }
            }
        }

        if (!continueWithPropertyPopulation) {
            return;
        }

        if (mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_NAME ||
                mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_TYPE) {
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
            // Add property values based on autowire by name if applicable.
            if (mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }

            // 此处逻辑复杂,不进行实现
            // Add property values based on autowire by type if applicable.
            if (mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }

            pvs = newPvs;
        }
        // 用后置处理器机制处理@Autowired注解,来创建属性的实例,然后再利用反射机制,将实例化好的属性,赋值给对象
        // 给属性赋值，当bp是AutowiredAnnotationBeanPostProcessor的时候，进入postProcessPropertyValues方法
        // 来到AutowiredAnnotationBeanPostProcessor的postProcessPropertyValues方法
        boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();

        if (hasInstAwareBpps ) {
            PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw);
            if (hasInstAwareBpps) {
                // /这里便是最最最重要的了,也就是最终的Autowired了.
                for (BeanPostProcessor bp : getBeanPostProcessors()) {
                    if (bp instanceof InstantiationAwareBeanPostProcessor) {
                        InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                        pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                        if (pvs == null) {
                            return;
                        }
                    }
                }
            }
        }

        // xml中配置的property注入在这里进行装配,注解注入不在pvs中,它是通过上面processor注入的
        applyPropertyValues(beanName, mbd, bw, pvs);
    }

    /**
     * 根据名称自动装配
     */
    protected void autowireByName(String beanName, RootBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
        // 解析出非简单属性
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            if (containsBean(propertyName)) {
                Object bean = getBean(propertyName);
                pvs.add(propertyName, bean);
                registerDependentBean(propertyName, beanName);
                if (logger.isDebugEnabled()) {
                    logger.debug("Added autowiring by name from bean name '" + beanName +
                            "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
                }
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                            "' by name: no matching bean found");
                }
            }
        }
    }

    /**
     * autowireByType太复杂.不做实现
     */
    protected void autowireByType(String beanName, RootBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
    }

    /**
     * Return an array of non-simple bean properties that are unsatisfied.
     * These are probably unsatisfied references to other beans in the
     * factory. Does not include simple properties like primitives or Strings.
     * @param mbd the merged bean definition the bean was created with
     * @param bw the BeanWrapper the bean was created with
     * @return an array of bean property names
     * @see org.springframework.beans.BeanUtils#isSimpleProperty
     */
    protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
        Set<String> result = new TreeSet<String>();
        PropertyValues pvs = mbd.getPropertyValues();
        PropertyDescriptor[] pds = bw.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
                    !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
                result.add(pd.getName());
            }
        }
        return StringUtils.toStringArray(result);
    }
    protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
        List<PropertyDescriptor> pds =
                new LinkedList<PropertyDescriptor>(Arrays.asList(bw.getPropertyDescriptors()));
        for (Iterator<PropertyDescriptor> it = pds.iterator(); it.hasNext();) {
            PropertyDescriptor pd = it.next();
            if (isExcludedFromDependencyCheck(pd)) {
                it.remove();
            }
        }
        return pds.toArray(new PropertyDescriptor[pds.size()]);
    }

    protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
        return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
                this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
                AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
    }


    /**
     * 对象的属性赋值
     */
    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        if (pvs == null || pvs.isEmpty()) {
            return;
        }
        MutablePropertyValues mpvs = null;
        List<PropertyValue> original;

        if (pvs instanceof MutablePropertyValues) {
            mpvs = (MutablePropertyValues) pvs;
            if (mpvs.isConverted()) {
                // Shortcut: use the pre-converted values as-is.
                try {
                    bw.setPropertyValues(mpvs);
                    return;
                }
                catch (BeansException ex) {
                    throw new BeanCreationException(
                            mbd.getResourceDescription(), beanName, "Error setting property values", ex);
                }
            }
            original = mpvs.getPropertyValueList();
        }
        else {
            original = Arrays.asList(pvs.getPropertyValues());
        }

        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }
        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

        // BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd);

        // Create a deep copy, resolving any references for values.
        List<PropertyValue> deepCopy = new ArrayList<PropertyValue>(original.size());
        boolean resolveNecessary = false;
        for (PropertyValue pv : original) {
            if (pv.isConverted()) {
                deepCopy.add(pv);
            } else {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
                Object convertedValue = resolvedValue;
                boolean convertible = !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
                if (convertible) {
                    // TODO 重要方法 进行属性转换
                    convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
                }
                // Possibly store converted value in merged bean definition,
                // in order to avoid re-conversion for every created bean instance.
                if (resolvedValue == originalValue) {
                    if (convertible) {
                        pv.setConvertedValue(convertedValue);
                    }
                    deepCopy.add(pv);
                } else if (convertible && originalValue instanceof TypedStringValue &&
                        !((TypedStringValue) originalValue).isDynamic() &&
                        !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
                    pv.setConvertedValue(convertedValue);
                    deepCopy.add(pv);
                }
                 else {
                    resolveNecessary = true;
                    deepCopy.add(new PropertyValue(pv, convertedValue));
                }
            }
        }
        if (mpvs != null && !resolveNecessary) {
            mpvs.setConverted();
        }

        // Set our (possibly massaged) deep copy.
        try {
            bw.setPropertyValues(new MutablePropertyValues(deepCopy)); // bean的属性赋值
        } catch (BeansException ex) {
            throw new BeanCreationException(beanName + "Error setting property values", ex);
        }
    }
    private Object convertForProperty(Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {
        if (converter instanceof BeanWrapperImpl) {
            return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
        }
        else {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
            return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
        }
    }
    /**
     * 执行Bean的初始化方法
     */
    protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {

        Object wrappedBean = bean;
        // TODO 前置处理器处理
        /*if (mbd == null ) {
            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        }*/

        try {
            invokeInitMethods(beanName, wrappedBean, mbd);
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName + "Invocation of init method failed", ex);
        }
        // TODO 后置处理器处理
        /*if (mbd == null ) {
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        }*/
        return wrappedBean;
    }

    /**
     * 调用初始化方法
     */
    protected void invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd)
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
    protected void invokeCustomInitMethod(String beanName, final Object bean, RootBeanDefinition mbd) throws Throwable {
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

}
