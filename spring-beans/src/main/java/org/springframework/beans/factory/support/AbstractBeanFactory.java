package org.springframework.beans.factory.support;

import org.springframework.beans.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.config.Scope;
import org.springframework.beans.exception.*;
import org.springframework.beans.factory.*;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyEditor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

    protected final Log logger = LogFactory.getLog(getClass());

    private BeanFactory parentBeanFactory;
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();
    private ClassLoader tempClassLoader;
    private ConversionService conversionService;
    private final Set<PropertyEditorRegistrar> propertyEditorRegistrars =
            new LinkedHashSet<PropertyEditorRegistrar>(4);
    private TypeConverter typeConverter;
    private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors =
            new HashMap<Class<?>, Class<? extends PropertyEditor>>(4);
    private BeanExpressionResolver beanExpressionResolver;
    private final ThreadLocal<Object> prototypesCurrentlyInCreation = new ThreadLocal<Object>();
    private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(64));
    private final Map<String, Scope> scopes = new LinkedHashMap<String, Scope>(8);
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();
    private boolean hasInstantiationAwareBeanPostProcessors;
    private boolean hasDestructionAwareBeanPostProcessors;
    private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<String, RootBeanDefinition>(64);

    //-----------------------------------------------------------------------------------
    // Implementation of BeanFactory interface开始
    //-----------------------------------------------------------------------------------

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

    @Override
    public boolean isCurrentlyInCreation(String beanName) {
        return false;
    }


    /**
     * 返回指定bean的实例,该实例可以是共享的,也可以是独立的。
     *
     * @param name要检索的bean的名称
     * @param required        type要检索的bean的必需类型
     * @param args            使用显式参数创建bean实例时要使用的参数 （仅用于创建新实例而不是检索现有实例）
     * @返回bean的实例
     * @如果无法创建bean，则抛出BeansException
     */
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
                throw new BeanCurrentlyInCreationException(beanName + "正在创建中");
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
                final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
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

                //单例,开始创建bean
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
                } else if (mbd.isPrototype()) {
                    // It's a prototype -> create a new instance.
                    Object prototypeInstance = null;
                    try {
                        //设置当前线程在create此bean
                        beforePrototypeCreation(beanName);
                        //和单例一个方法，后面会分析到
                        prototypeInstance = createBean(beanName, mbd, args); // 由子类实现
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
                                    return createBean(beanName, mbd, args); // 由子类实现
                                } finally {
                                    afterPrototypeCreation(beanName);
                                }
                            }
                        });
                        bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                    } catch (IllegalStateException ex) {
                        throw new BeanCreationException(beanName + "当前线程不支持'" + scopeName + "'作用域 ", ex);
                    }
                }
            } catch (BeansException ex) {
                cleanupAfterBeanCreationFailure(beanName);
                throw ex;
            }
        }

        return (T) bean;
    }


    @Override
    public boolean containsBean(String name) {
        String beanName = name;
        if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
            return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
        }
        // Not found -> check parent.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        return (parentBeanFactory != null && parentBeanFactory.containsBean(name));
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);

        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            if (beanInstance instanceof FactoryBean) {
                return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
            } else {
                return !BeanFactoryUtils.isFactoryDereference(name);
            }
        } else if (containsSingleton(beanName)) {
            return true;
        } else {
            // No singleton instance found -> check bean definition.
            BeanFactory parentBeanFactory = getParentBeanFactory();
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
                // No bean definition found in this factory -> delegate to parent.
                return parentBeanFactory.isSingleton(originalBeanName(name));
            }

            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

            // In case of FactoryBean, return singleton status of created object if not a dereference.
            if (mbd.isSingleton()) {
                if (isFactoryBean(beanName, mbd)) {
                    if (BeanFactoryUtils.isFactoryDereference(name)) {
                        return true;
                    }
                    FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                    return factoryBean.isSingleton();
                } else {
                    return !BeanFactoryUtils.isFactoryDereference(name);
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);

        // Check manually registered singletons.
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            if (beanInstance instanceof FactoryBean) {
                if (!BeanFactoryUtils.isFactoryDereference(name)) {
                    Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
                    return (type != null && typeToMatch.isAssignableFrom(type));
                }
                else {
                    return typeToMatch.isInstance(beanInstance);
                }
            }
            else {
                return (!BeanFactoryUtils.isFactoryDereference(name) && typeToMatch.isInstance(beanInstance));
            }
        }
        else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
            // null instance registered
            return false;
        }

        else {
            // No singleton instance found -> check bean definition.
            BeanFactory parentBeanFactory = getParentBeanFactory();
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
                // No bean definition found in this factory -> delegate to parent.
                return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
            }

            // Retrieve corresponding bean definition.
            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

            Class<?> classToMatch = typeToMatch.getRawClass();
            Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
                    new Class<?>[] {classToMatch} : new Class<?>[] {FactoryBean.class, classToMatch});

            // Check decorated bean definition, if any: We assume it'll be easier
            // to determine the decorated bean's type than the proxy's type.
            BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
            if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
                RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
                Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
                if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
                    return typeToMatch.isAssignableFrom(targetClass);
                }
            }

            Class<?> beanType = predictBeanType(beanName, mbd, typesToMatch);
            if (beanType == null) {
                return false;
            }

            // Check bean class whether we're dealing with a FactoryBean.
            if (FactoryBean.class.isAssignableFrom(beanType)) {
                if (!BeanFactoryUtils.isFactoryDereference(name)) {
                    // If it's a FactoryBean, we want to look at what it creates, not the factory class.
                    beanType = getTypeForFactoryBean(beanName, mbd);
                    if (beanType == null) {
                        return false;
                    }
                }
            }
            else if (BeanFactoryUtils.isFactoryDereference(name)) {
                // Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
                // type but we nevertheless are being asked to dereference a FactoryBean...
                // Let's check the original bean class and proceed with it if it is a FactoryBean.
                beanType = predictBeanType(beanName, mbd, FactoryBean.class);
                if (beanType == null || !FactoryBean.class.isAssignableFrom(beanType)) {
                    return false;
                }
            }

            return typeToMatch.isAssignableFrom(beanType);
        }
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);

        // Check manually registered singletons.
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
                return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
            } else {
                return beanInstance.getClass();
            }
        } else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
            // null instance registered
            return null;
        } else {
            // No singleton instance found -> check bean definition.
            BeanFactory parentBeanFactory = getParentBeanFactory();
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
                // No bean definition found in this factory -> delegate to parent.
                return parentBeanFactory.getType(originalBeanName(name));
            }

            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

            // Check decorated bean definition, if any: We assume it'll be easier
            // to determine the decorated bean's type than the proxy's type.
            BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
            if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
                RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
                Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
                if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
                    return targetClass;
                }
            }

            Class<?> beanClass = predictBeanType(beanName, mbd);

            // Check bean class whether we're dealing with a FactoryBean.
            if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
                if (!BeanFactoryUtils.isFactoryDereference(name)) {
                    // If it's a FactoryBean, we want to look at what it creates, not at the factory class.
                    return getTypeForFactoryBean(beanName, mbd);
                } else {
                    return beanClass;
                }
            } else {
                return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
            }
        }
    }

    //-----------------------------------------------------------------------------------
    // Implementation of HierarchicalBeanFactory interface开始
    //-----------------------------------------------------------------------------------

    public BeanFactory getParentBeanFactory() {
        return this.parentBeanFactory;
    }

    public boolean containsLocalBean(String name) {
        String beanName = transformedBeanName(name);
        return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
                (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
    }

    //---------------------------------------------------------------------
    // Implementation of ConfigurableBeanFactory interface
    //---------------------------------------------------------------------
    public void setParentBeanFactory(BeanFactory parentBeanFactory) {
        if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
            throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
        }
        this.parentBeanFactory = parentBeanFactory;
    }

    public ClassLoader getBeanClassLoader() {
        return this.beanClassLoader;
    }

    public ClassLoader getTempClassLoader() {
        return this.tempClassLoader;
    }

    @Override
    public void setTempClassLoader(ClassLoader tempClassLoader) {
        this.tempClassLoader = tempClassLoader;
    }

    @Override
    public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
        Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
        this.propertyEditorRegistrars.add(registrar);
    }

    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
        if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
            this.hasInstantiationAwareBeanPostProcessors = true;
        }
        if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
            this.hasDestructionAwareBeanPostProcessors = true;
        }
    }

    @Override
    public int getBeanPostProcessorCount() {
        return this.beanPostProcessors.size();
    }

    public Scope getRegisteredScope(String scopeName) {
        Assert.notNull(scopeName, "Scope identifier must not be null");
        return this.scopes.get(scopeName);
    }

    public ConversionService getConversionService() {
        return this.conversionService;
    }

    protected TypeConverter getCustomTypeConverter() {
        return this.typeConverter;
    }

    @Override
    public TypeConverter getTypeConverter() {
        TypeConverter customConverter = getCustomTypeConverter();
        if (customConverter != null) {
            return customConverter;
        } else {
            // Build default TypeConverter, registering custom editors.
            SimpleTypeConverter typeConverter = new SimpleTypeConverter();
            typeConverter.setConversionService(getConversionService());
            registerCustomEditors(typeConverter);
            return typeConverter;
        }
    }
    //-----------------------------------------------------------------------------------
    // Implementation of HierarchicalBeanFactory interface结束
    //-----------------------------------------------------------------------------------


    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }

    protected boolean hasInstantiationAwareBeanPostProcessors() {
        return this.hasInstantiationAwareBeanPostProcessors;
    }

    // @Override
    public boolean isFactoryBean(String name) {
        String beanName = name;

        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            return (beanInstance instanceof FactoryBean);
        } else if (containsSingleton(beanName)) {
            return false;
        }

        Class<?> beanType = beanInstance.getClass();
        return (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
    }

    protected boolean isPrototypeCurrentlyInCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        return (curVal != null &&
                (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
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

    protected boolean hasDestructionAwareBeanPostProcessors() {
        return this.hasDestructionAwareBeanPostProcessors;
    }

    protected String transformedBeanName(String name) {
        return BeanFactoryUtils.transformedBeanName(name);
    }

    protected String originalBeanName(String name) {
        String beanName = transformedBeanName(name);
        if (name.startsWith(FACTORY_BEAN_PREFIX)) {
            beanName = FACTORY_BEAN_PREFIX + beanName;
        }
        return beanName;
    }

    protected void registerCustomEditors(PropertyEditorRegistry registry) {
        PropertyEditorRegistrySupport registrySupport =
                (registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
        if (registrySupport != null) {
            registrySupport.useConfigValueEditors();
        }
        if (!this.propertyEditorRegistrars.isEmpty()) {
            for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
                try {
                    registrar.registerCustomEditors(registry);
                } catch (BeanCreationException ex) {
                    Throwable rootCause = ex.getMostSpecificCause();
                    if (rootCause instanceof BeanCurrentlyInCreationException) {
                        BeanCreationException bce = (BeanCreationException) rootCause;
                        if (isCurrentlyInCreation(bce.getBeanName())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
                                        "] failed because it tried to obtain currently created bean '" +
                                        ex.getBeanName() + "': " + ex.getMessage());
                            }
                            onSuppressedException(ex);
                            continue;
                        }
                    }
                    throw ex;
                }
            }
        }
        if (!this.customEditors.isEmpty()) {
            for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
                Class<?> requiredType = entry.getKey();
                Class<? extends PropertyEditor> editorClass = entry.getValue();
                registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass));
            }
        }
    }

    /**
     * 初始化BeanWrapper,此处暂不处理
     * Initialize the given BeanWrapper with the custom editors registered
     * with this factory. To be called for BeanWrappers that will create
     * and populate bean instances.
     * <p>The default implementation delegates to {@link #registerCustomEditors}.
     * Can be overridden in subclasses.
     *
     * @param bw the BeanWrapper to initialize
     */
    protected void initBeanWrapper(BeanWrapper bw) {
        bw.setConversionService(getConversionService());
        registerCustomEditors(bw);
    }

    protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
        // Quick check on the concurrent map first, with minimal locking.
        RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
        if (mbd != null) {
            return mbd;
        }
        return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
    }

    protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) {
      /*  final BeanDefinition bd = getBeanDefinition(beanName);
        RootBeanDefinition mbd = new RootBeanDefinition();
        BeanUtils.copyProperties(bd, mbd);
        mbd.setDependsOn(bd.getDependsOn());
        return mbd;*/
        return getMergedBeanDefinition(beanName, bd, null);
    }

    protected RootBeanDefinition getMergedBeanDefinition(
            String beanName, BeanDefinition bd, BeanDefinition containingBd)
            throws BeanDefinitionStoreException {

        synchronized (this.mergedBeanDefinitions) {
            RootBeanDefinition mbd = null;

            // Check with full lock now in order to enforce the same merged instance.
            if (containingBd == null) {
                mbd = this.mergedBeanDefinitions.get(beanName);
            }

            if (mbd == null) {
                if (bd.getParentName() == null) {
                    // Use copy of given root bean definition.
                    if (bd instanceof RootBeanDefinition) {
                        mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
                    } else {
                        mbd = new RootBeanDefinition(bd);
                    }
                }

                // Set default singleton scope, if not configured before.
                if (!StringUtils.hasLength(mbd.getScope())) {
                    mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
                }

                // A bean contained in a non-singleton bean cannot be a singleton itself.
                // Let's correct this on the fly here, since this might be the result of
                // parent-child merging for the outer bean, in which case the original inner bean
                // definition will not have inherited the merged outer bean's singleton status.
                if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
                    mbd.setScope(containingBd.getScope());
                }

                // Only cache the merged bean definition if we're already about to create an
                // instance of the bean, or at least have already created an instance before.
                if (containingBd == null /*&& isCacheBeanMetadata()*/) {
                    this.mergedBeanDefinitions.put(beanName, mbd);
                }
            }

            return mbd;
        }
    }

    public void clearMetadataCache() {
        Iterator<String> mergedBeans = this.mergedBeanDefinitions.keySet().iterator();
        while (mergedBeans.hasNext()) {
            if (!isBeanEligibleForMetadataCaching(mergedBeans.next())) {
                mergedBeans.remove();
            }
        }
    }

    /**
     * Resolve the bean class for the specified bean definition,
     * resolving a bean class name into a Class reference (if necessary)
     * and storing the resolved Class in the bean definition for further use.
     *
     * @param mbd          the merged bean definition to determine the class for
     * @param beanName     the name of the bean (for error handling purposes)
     * @param typesToMatch the types to match in case of internal type matching purposes
     *                     (also signals that the returned {@code Class} will never be exposed to application code)
     * @return the resolved bean class (or {@code null} if none)
     * @throws CannotLoadBeanClassException if we failed to load the class
     */
    protected Class<?> resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch)
            throws CannotLoadBeanClassException {
        try {
            if (mbd.hasBeanClass()) {
                return mbd.getBeanClass();
            }
            return doResolveBeanClass(mbd, typesToMatch);
        } catch (ClassNotFoundException ex) {
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
        } catch (LinkageError err) {
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
        }
    }

    private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch) throws ClassNotFoundException {
        ClassLoader beanClassLoader = getBeanClassLoader();
        ClassLoader classLoaderToUse = beanClassLoader;
        if (!ObjectUtils.isEmpty(typesToMatch)) {
            // When just doing type checks (i.e. not creating an actual instance yet),
            // use the specified temporary class loader (e.g. in a weaving scenario).
            ClassLoader tempClassLoader = getTempClassLoader();
            if (tempClassLoader != null) {
                classLoaderToUse = tempClassLoader;
                if (tempClassLoader instanceof DecoratingClassLoader) {
                    DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
                    for (Class<?> typeToMatch : typesToMatch) {
                        dcl.excludeClass(typeToMatch.getName());
                    }
                }
            }
        }
        String className = mbd.getBeanClassName();
        if (className != null) {
            Object evaluated = evaluateBeanDefinitionString(className, mbd);
            if (!className.equals(evaluated)) {
                // A dynamically resolved expression, supported as of 4.2...
                if (evaluated instanceof Class) {
                    return (Class<?>) evaluated;
                } else if (evaluated instanceof String) {
                    return ClassUtils.forName((String) evaluated, classLoaderToUse);
                } else {
                    throw new IllegalStateException("Invalid class name expression result: " + evaluated);
                }
            }
            // When resolving against a temporary class loader, exit early in order
            // to avoid storing the resolved Class in the bean definition.
            if (classLoaderToUse != beanClassLoader) {
                return ClassUtils.forName(className, classLoaderToUse);
            }
        }
        return mbd.resolveBeanClass(beanClassLoader);
    }

    /**
     * Evaluate the given String as contained in a bean definition,
     * potentially resolving it as an expression.
     *
     * @param value          the value to check
     * @param beanDefinition the bean definition that the value comes from
     * @return the resolved value
     * @see #setBeanExpressionResolver
     */
    protected Object evaluateBeanDefinitionString(String value, BeanDefinition beanDefinition) {
        if (this.beanExpressionResolver == null) {
            return value;
        }
        Scope scope = (beanDefinition != null ? getRegisteredScope(beanDefinition.getScope()) : null);
        return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
    }

    protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        if (mbd.getFactoryMethodName() != null) {
            return null;
        }
        return resolveBeanClass(mbd, beanName, typesToMatch);
    }

    protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
        Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
        return (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
    }

    protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
        if (!mbd.isSingleton()) {
            return null;
        }
        try {
            FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
            return factoryBean.getObjectType();
        } catch (BeanCreationException ex) {
            if (ex instanceof BeanCurrentlyInCreationException) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bean currently in creation on FactoryBean type check: " + ex);
                }
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("Bean creation exception on FactoryBean type check: " + ex);
                }
            }
            return null;
        }
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
     * Perform appropriate cleanup of cached metadata after bean creation failed.
     *
     * @param beanName the name of the bean
     */
    protected void cleanupAfterBeanCreationFailure(String beanName) {
        this.alreadyCreated.remove(beanName);
    }

    protected boolean isBeanEligibleForMetadataCaching(String beanName) {
        return this.alreadyCreated.contains(beanName);
    }

    /**
     * Remove the singleton instance (if any) for the given bean name,
     * but only if it hasn't been used for other purposes than type checking.
     *
     * @param beanName the name of the bean
     * @return {@code true} if actually removed, {@code false} otherwise
     */
    protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
        if (!this.alreadyCreated.contains(beanName)) {
            removeSingleton(beanName);
            return true;
        } else {
            return false;
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
            Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {

        // Don't let calling code try to dereference the factory if the bean isn't a factory.
        if (BeanFactoryUtils.isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
            throw new BeanIsNotAFactoryException(name + "不是一个FactoryBean", beanInstance.getClass());
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

    public boolean isBeanNameInUse(String beanName) {
        return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
    }

    protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
        return (bean != null &&
                (DisposableBeanAdapter.hasDestroyMethod(bean, mbd) || hasDestructionAwareBeanPostProcessors()));
    }

    /**
     * Add the given bean to the list of disposable beans in this factory,
     * registering its DisposableBean interface and/or the given destroy method
     * to be called on factory shutdown (if applicable). Only applies to singletons.
     *
     * @param beanName the name of the bean
     * @param bean     the bean instance
     * @param mbd      the bean definition for the bean
     * @see RootBeanDefinition#isSingleton
     * @see RootBeanDefinition#getDependsOn
     * @see #registerDisposableBean
     * @see #registerDependentBean
     */
    protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
        if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
            if (mbd.isSingleton()) {
                // Register a DisposableBean implementation that performs all destruction
                // work for the given bean: DestructionAwareBeanPostProcessors,
                // DisposableBean interface, custom destroy method.
                registerDisposableBean(beanName,
                        new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors()));
            } else {
                // A bean with a custom scope...
                Scope scope = this.scopes.get(mbd.getScope());
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope '" + mbd.getScope() + "'");
                }
                scope.registerDestructionCallback(beanName,
                        new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors()));
            }
        }
    }
    /**
     * 抽象方法宫子类实现
     */

    /**
     * Check if this bean factory contains a bean definition with the given name.
     * Does not consider any hierarchy this factory may participate in.
     * Invoked by {@code containsBean} when no cached singleton instance is found.
     * <p>Depending on the nature of the concrete bean factory implementation,
     * this operation might be expensive (for example, because of directory lookups
     * in external registries). However, for listable bean factories, this usually
     * just amounts to a local hash lookup: The operation is therefore part of the
     * public interface there. The same implementation can serve for both this
     * template method and the public interface method in that case.
     *
     * @param beanName the name of the bean to look for
     * @return if this bean factory contains a bean definition with the given name
     * @see #containsBean
     * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
     */
    protected abstract boolean containsBeanDefinition(String beanName);

    /**
     * Return the bean definition for the given bean name.
     * Subclasses should normally implement caching, as this method is invoked
     * by this class every time bean definition metadata is needed.
     * <p>Depending on the nature of the concrete bean factory implementation,
     * this operation might be expensive (for example, because of directory lookups
     * in external registries). However, for listable bean factories, this usually
     * just amounts to a local hash lookup: The operation is therefore part of the
     * public interface there. The same implementation can serve for both this
     * template method and the public interface method in that case.
     *
     * @param beanName the name of the bean to find a definition for
     * @return the BeanDefinition for this prototype name (never {@code null})
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if the bean definition cannot be resolved
     * @throws BeansException                                                  in case of errors
     * @see RootBeanDefinition
     * @see ChildBeanDefinition
     * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
     */
    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    /**
     * Create a bean instance for the given merged bean definition (and arguments).
     * The bean definition will already have been merged with the parent definition
     * in case of a child definition.
     * <p>All bean retrieval methods delegate to this method for actual bean creation.
     *
     * @param beanName the name of the bean
     * @param mbd      the merged bean definition for the bean
     * @param args     explicit arguments to use for constructor or factory method invocation
     * @return a new instance of the bean
     * @throws BeanCreationException if the bean could not be created
     */
    protected abstract Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException;

}
