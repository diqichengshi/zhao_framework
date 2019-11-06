package org.springframework.beans.factory.support;

import org.springframework.beans.config.AutowireCapableBeanFactory;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.exception.BeanCurrentlyInCreationException;
import org.springframework.beans.BeansException;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.PriorityOrdered;
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
import java.util.concurrent.ConcurrentHashMap;


/**
 * Abstract bean factory superclass that implements default bean creation,
 * with the full capabilities specified by the {@link RootBeanDefinition} class.
 * Implements the {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
 * interface in addition to AbstractBeanFactory's {@link #createBean} method.
 *
 * <p>Provides bean creation (with constructor resolution), property population,
 * wiring (including autowiring), and initialization. Handles runtime bean
 * references, resolves managed collections, calls initialization methods, etc.
 * Supports autowiring constructors, properties by name, and properties by type.
 *
 * <p>The main template method to be implemented by subclasses is
 * {@link #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)},
 * used for autowiring by type. In case of a factory which is capable of searching
 * its bean definitions, matching beans will typically be implemented through such
 * a search. For other factory styles, simplified matching algorithms can be implemented.
 *
 * <p>Note that this class does <i>not</i> assume or implement bean definition
 * registry capabilities. See {@link DefaultListableBeanFactory} for an implementation
 * of the {@link org.springframework.beans.factory.ListableBeanFactory} and
 * {@link BeanDefinitionRegistry} interfaces, which represent the API and SPI
 * view of such a factory, respectively.
 *
 * 自动装配以及属性设置
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Chris Beams
 * @author Sam Brannen
 * @since 13.02.2004
 * @see RootBeanDefinition
 * @see DefaultListableBeanFactory
 * @see BeanDefinitionRegistry
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private boolean allowCircularReferences = true;
    private boolean allowRawInjectionDespiteWrapping = false;
    private final Set<Class<?>> ignoredDependencyTypes = new HashSet<Class<?>>();
    private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<Class<?>>();
    private final Map<String, BeanWrapper> factoryBeanInstanceCache =
            new ConcurrentHashMap<String, BeanWrapper>(16);

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
        RootBeanDefinition mbdToUse = mbd;

        // Make sure bean class is actually resolved at this point, and
        // clone the bean definition in case of a dynamically resolved Class
        // which cannot be stored in the shared merged bean definition.
        Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            mbdToUse = new RootBeanDefinition(mbd);
            mbdToUse.setBeanClass(resolvedClass);
        }

        // Prepare method overrides.
        try {
            mbdToUse.prepareMethodOverrides();
        } catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                    beanName, "Validation of method overrides failed", ex);
        }

        /*try {
            // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                return bean;
            }
        }
        catch (Throwable ex) {
            throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                    "BeanPostProcessor before instantiation of bean failed", ex);
        }*/

        Object beanInstance = doCreateBean(beanName, mbdToUse, args);
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
        // 先进行实例化bean(Instantiate the bean).
        BeanWrapper instanceWrapper = null;
        if (mbd.isSingleton()) {
            // 移除BeanWrapper缓存
            instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
        }
        if (instanceWrapper == null) {
            // 创建BeanWrapper,这是一个被包装过了的 bean,它里面的属性还未赋实际值
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }
        // 获取bean实例和实例化对象的类型
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

        // Eagerly cache singletons to be able to resolve circular references
        // even when triggered by lifecycle interfaces like BeanFactoryAware.
        // 下面代码是为了解决循环依赖的问题
        boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                isSingletonCurrentlyInCreation(beanName));
        // 提前曝光bean
        if (earlySingletonExposure) {
            if (logger.isDebugEnabled()) {
                logger.debug("Eagerly caching bean '" + beanName +
                        "' to allow for resolving potential circular references");
            }
            // 该方法介于bean实例化和依赖注入(populateBean)中间,这个对象已经被生产出来了,虽然还没有真正初始化,
            // 但是可以根据对象引用能定位到堆中的对象,所以Spring此时将这个对象提前曝光出来让大家认识
            addSingletonFactory(beanName, new ObjectFactory<Object>() {
                @Override
                public Object getObject() throws BeansException {
                    return getEarlyBeanReference(beanName, mbd, bean);
                }
            });
        }

        // 初始化Bean实例
        Object exposedObject = bean;
        try {
            //TODO 实例化后,需要进行依赖注入(属性填充,自动注入)
            populateBean(beanName, mbd, instanceWrapper);
            if (exposedObject != null) {
                // TODO 这里就是处理bean初始化完成后的各种回调,例如init-method配置,BeanPostProcessor接口
                exposedObject = initializeBean(mbd.getResourceDescription(), exposedObject, mbd);
            }
        } catch (Throwable ex) {
            if (ex instanceof BeanCreationException) {
                throw (BeanCreationException) ex;
            } else {
                throw new BeanCreationException(beanName + "Initialization of bean failed", ex);
            }
        }

        if (earlySingletonExposure) {
            // 如果已经提交曝光了bean,那么就从缓存中获取bean
            Object earlySingletonReference = getSingleton(beanName, false);
            if (earlySingletonReference != null) {
                // /根据名称获取的已注册的Bean和正在实例化的Bean是同一个
                if (exposedObject == bean) {
                    exposedObject = earlySingletonReference;
                } else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                    String[] dependentBeans = getDependentBeans(beanName);
                    Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
                    for (String dependentBean : dependentBeans) {
                        if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                            actualDependentBeans.add(dependentBean);
                        }
                    }
                    if (!actualDependentBeans.isEmpty()) {
                        throw new BeanCurrentlyInCreationException(beanName,
                                "Bean with name '" + beanName + "' has been injected into other beans [" +
                                        StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                                        "] in its raw version as part of a circular reference, but has eventually been " +
                                        "wrapped. This means that said other beans do not use the final version of the " +
                                        "bean. This is often the result of over-eager type matching - consider using " +
                                        "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                    }
                }
            }
        }
        // Register bean as disposable.
        try {
            registerDisposableBeanIfNecessary(beanName, bean, mbd);
        }
        catch (BeanDefinitionValidationException ex) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
        }

        return exposedObject;
    }

    /**
     * Obtain a reference for early access to the specified bean,
     * typically for the purpose of resolving a circular reference.
     *
     * @param beanName the name of the bean (for error handling purposes)
     * @param mbd      the merged bean definition for the bean
     * @param bean     the raw bean instance
     * @return the object to expose as bean reference
     */
    protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
        Object exposedObject = bean;
        if (bean != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
                    if (exposedObject == null) {
                        return exposedObject;
                    }
                }
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
            } else {
                return instantiateBean(beanName, mbd);
            }
        }

        // Need to determine the constructor...
        Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
        if (ctors != null ||
                mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
                mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
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

        // 给InstantiationAwareBeanPostProcessors最后一次机会在属性注入前修改Bean的属性值
        // 具体通过调用postProcessAfterInstantiation方法,如果调用返回false,表示不必继续进行依赖注入,直接返回
        // 比如利用后置处理器机制,用来处理@AutoWired注解
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
        // 根据Bean配置的依赖注入方式完成注入,默认是0,即不走以下逻辑,所有的依赖注入都需要在xml文件中有显式的配置
        // 如果设置了相关的依赖装配方式,会遍历Bean中的属性,根据类型或名称来完成相应注入,无需额外配置
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
                mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
            // 深拷贝当前已有的配置
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
            // 根据名称进行注入
            if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }

            // // 根据类型进行注入
            // Add property values based on autowire by type if applicable.
            if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }

            pvs = newPvs;
        }
        // 用后置处理器机制处理@Autowired注解,来创建属性的实例,然后再利用反射机制,将实例化好的属性,赋值给对象
        // 给属性赋值，当bp是AutowiredAnnotationBeanPostProcessor的时候，进入postProcessPropertyValues方法
        // 来到AutowiredAnnotationBeanPostProcessor的postProcessPropertyValues方法
        boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
        // 是否进行依赖检查
        boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);
        if (hasInstAwareBpps) {
            // 过滤出所有需要进行依赖检查的属性编辑器
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
            if (needsDepCheck) {
                // 检查是否满足相关依赖关系,对应的depends-on属性,需要确保所有依赖的Bean先完成初始化
                checkDependencies(beanName, mbd, filteredPds, pvs);
            }
        }

        // xml中配置的property注入在这里进行装配,注解注入不在pvs中,它是通过上面processor注入的
        applyPropertyValues(beanName, mbd, bw, pvs);
    }

    /**
     * 根据名称自动装配
     */
    protected void autowireByName(String beanName, RootBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
        // 根据bw的PropertyDescriptors,遍历出所有可写的(即set方法存在),存在于BeanDefinition里的PropertyValues,且不是简单属性的属性名
        // 简单属性的判定参照下面方法,主要涵盖基本类型及其包装类,Number,Date等
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            if (containsBean(propertyName)) {
                // 递归初始化相关Bean
                Object bean = getBean(propertyName);
                pvs.add(propertyName, bean);
                // 注册依赖关系
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
     * Abstract method defining "autowire by type" (bean properties by type) behavior.
     * <p>This is like PicoContainer default, in which there must be exactly one bean
     * of the property type in the bean factory. This makes bean factories simple to
     * configure for small namespaces, but doesn't work as well as standard Spring
     * behavior for bigger applications.
     * @param beanName the name of the bean to autowire by type
     * @param mbd the merged bean definition to update through autowiring
     * @param bw BeanWrapper from which we can obtain information about the bean
     * @param pvs the PropertyValues to register wired objects with
     */
    protected void autowireByType(String beanName, RootBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
        // 获取类型转换器
        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }

        Set<String> autowiredBeanNames = new LinkedHashSet<String>(4);
        // 根据bw的PropertyDescriptors,遍历出所有可写的(即set方法存在),存在于BeanDefinition里的PropertyValues,且不是简单属性的属性名
        // 简单属性的判定参照下面方法,主要涵盖基本类型及其包装类,Number,Date等
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            try {
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                // Don't try autowiring by type for type Object: never makes sense,
                // even if it technically is a unsatisfied, non-simple property.
                // 如果是Object类型不进行装配
                if (Object.class != pd.getPropertyType()) {
                    MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                    // Do not allow eager init (懒加载) for type matching in case of a prioritized post-processor.
                    // 定义是否允许懒加载
                    boolean eager = !PriorityOrdered.class.isAssignableFrom(bw.getWrappedClass());
                    DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                    // 这里会根据传入desc里的入参类型,作为依赖装配的类型
                    // 再根据这个类型在BeanFacoty中查找所有类或其父类相同的BeanName
                    // 最后根据BeanName获取或初始化相应的类,然后将所有满足条件的BeanName填充到autowiredBeanNames中。
                    Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                    if (autowiredArgument != null) {
                        pvs.add(propertyName, autowiredArgument);
                    }
                    for (String autowiredBeanName : autowiredBeanNames) {
                        // 注册依赖
                        registerDependentBean(autowiredBeanName, beanName);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Autowiring by type from bean name '" + beanName + "' via property '" +
                                    propertyName + "' to bean named '" + autowiredBeanName + "'");
                        }
                    }
                    autowiredBeanNames.clear();
                }
            }
            catch (BeansException ex) {
                throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
            }
        }
    }

    /**
     * Return an array of non-simple bean properties that are unsatisfied.
     * These are probably unsatisfied references to other beans in the
     * factory. Does not include simple properties like primitives or Strings.
     *
     * @param mbd the merged bean definition the bean was created with
     * @param bw  the BeanWrapper the bean was created with
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
    /**过滤出所有需要进行依赖检查的属性编辑器*/
    protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
        List<PropertyDescriptor> pds =
                new LinkedList<PropertyDescriptor>(Arrays.asList(bw.getPropertyDescriptors()));
        for (Iterator<PropertyDescriptor> it = pds.iterator(); it.hasNext(); ) {
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
     * Perform a dependency check that all properties exposed have been set,
     * if desired. Dependency checks can be objects (collaborating beans),
     * simple (primitives and String), or all (both).
     * @param beanName the name of the bean
     * @param mbd the merged bean definition the bean was created with
     * @param pds the relevant property descriptors for the target bean
     * @param pvs the property values to be applied to the bean
     * @see #isExcludedFromDependencyCheck(java.beans.PropertyDescriptor)
     */
    protected void checkDependencies(
            String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds, PropertyValues pvs)
            throws UnsatisfiedDependencyException {

        int dependencyCheck = mbd.getDependencyCheck();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && !pvs.contains(pd.getName())) {
                boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
                boolean unsatisfied = (dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_ALL) ||
                        (isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_SIMPLE) ||
                        (!isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
                if (unsatisfied) {
                    throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(),
                            "Set this property value or disable dependency checking for this bean.");
                }
            }
        }
    }
    /**
     * Apply the given property values, resolving any runtime references
     * to other beans in this bean factory. Must use deep copy, so we
     * don't permanently modify this property.
     * 对象的属性赋值
     * @param beanName the bean name passed for better exception information
     * @param mbd the merged bean definition
     * @param bw the BeanWrapper wrapping the target object
     * @param pvs the new property values
     */
    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        // 为空直接返回
        if (pvs == null || pvs.isEmpty()) {
            return;
        }
        MutablePropertyValues mpvs = null;
        List<PropertyValue> original;

        if (pvs instanceof MutablePropertyValues) {
            mpvs = (MutablePropertyValues) pvs;
            if (mpvs.isConverted()) {
                // 如果已被设置转换完成，直接完成配置
                try {
                    bw.setPropertyValues(mpvs);
                    return;
                } catch (BeansException ex) {
                    throw new BeanCreationException(
                            mbd.getResourceDescription(), beanName, "Error setting property values", ex);
                }
            }
            original = mpvs.getPropertyValueList();
        } else {
            original = Arrays.asList(pvs.getPropertyValues());
        }

        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }
        // 创建BeanDefinitionValueResolver解析器,用来解析未被解析的PropertyValue.
        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

        // Create a deep copy, resolving any references for values.
        // 开始遍历检查original中的属性,对未被解析的先解析/已解析的直接加入deepCopy中,最后再填充到具体的Bean实例中
        List<PropertyValue> deepCopy = new ArrayList<PropertyValue>(original.size());
        boolean resolveNecessary = false;
        for (PropertyValue pv : original) {
            // 如果属性已经转化，直接添加
            if (pv.isConverted()) {
                deepCopy.add(pv);
            } else {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                // TODO 核心逻辑,解析获取实际的值
                // 对于RuntimeReference,会解析拿到具体的beanName,最终通过getBean(beanName)拿到具体的对象
                Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
                Object convertedValue = resolvedValue;
                // 判断是否可以转换
                boolean convertible = !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
                if (convertible) {
                    // 重要方法 尝试进行属性转换
                    convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
                }
                // 避免需要重复转换,设定已转换
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
                } else {
                    resolveNecessary = true;
                    deepCopy.add(new PropertyValue(pv, convertedValue));
                }
            }
        }
        if (mpvs != null && !resolveNecessary) {
            mpvs.setConverted();
        }
        // TODO 完成属性的设置
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
        } else {
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

    private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

        public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
            super(methodParameter, false, eager);
        }

        @Override
        public String getDependencyName() {
            return null;
        }
    }
}
