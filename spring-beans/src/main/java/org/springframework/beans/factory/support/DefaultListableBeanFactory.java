package org.springframework.beans.factory.support;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.exception.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

    private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories =
            new ConcurrentHashMap<String, Reference<DefaultListableBeanFactory>>(8);
    private String serializationId;
    private boolean allowEagerClassLoading = true;
    private boolean allowBeanDefinitionOverriding = true;
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();
    private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<Class<?>, String[]>(64);
    private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<Class<?>, String[]>(64);
    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<Class<?>, Object>(16);
    private Comparator<Object> dependencyComparator;
    private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();
    private volatile List<String> beanDefinitionNames = new ArrayList<String>(64);
    private volatile Set<String> manualSingletonNames = new LinkedHashSet<String>(16);
    private volatile String[] frozenBeanDefinitionNames;
    private volatile boolean configurationFrozen = false;

    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public DefaultListableBeanFactory() {
        super();
    }

    public DefaultListableBeanFactory(BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    public void setSerializationId(String serializationId) {
        if (serializationId != null) {
            serializableFactories.put(serializationId, new WeakReference<DefaultListableBeanFactory>(this));
        }
        else if (this.serializationId != null) {
            serializableFactories.remove(this.serializationId);
        }
        this.serializationId = serializationId;
    }

    public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
        this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
    }

    public boolean isAllowBeanDefinitionOverriding() {
        return this.allowBeanDefinitionOverriding;
    }

    public boolean isAllowEagerClassLoading() {
        return this.allowEagerClassLoading;
    }

    public Comparator<Object> getDependencyComparator() {
        return this.dependencyComparator;
    }

    public AutowireCandidateResolver getAutowireCandidateResolver() {
        return this.autowireCandidateResolver;
    }



    //---------------------------------------------------------------------
    // Implementation of remaining BeanFactory methods
    //---------------------------------------------------------------------

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return getBean(requiredType, (Object[]) null);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        Assert.notNull(requiredType, "Required type must not be null");
        String[] beanNames = getBeanNamesForType(requiredType);
        if (beanNames.length > 1) {
            ArrayList<String> autowireCandidates = new ArrayList<String>();
            for (String beanName : beanNames) {
                if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
                    autowireCandidates.add(beanName);
                }
            }
            if (autowireCandidates.size() > 0) {
                beanNames = autowireCandidates.toArray(new String[autowireCandidates.size()]);
            }
        }
        if (beanNames.length == 1) {
            return getBean(beanNames[0], requiredType, args);
        }
        else if (beanNames.length > 1) {
            Map<String, Object> candidates = new HashMap<String, Object>();
            for (String beanName : beanNames) {
                candidates.put(beanName, getBean(beanName, requiredType, args));
            }
            String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
            if (primaryCandidate != null) {
                return getBean(primaryCandidate, requiredType, args);
            }
            String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
            if (priorityCandidate != null) {
                return getBean(priorityCandidate, requiredType, args);
            }
            throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
        }
        else if (getParentBeanFactory() != null) {
            return getParentBeanFactory().getBean(requiredType, args);
        }
        else {
            throw new NoSuchBeanDefinitionException(requiredType);
        }
    }
    //---------------------------------------------------------------------
    // Implementation of ListableBeanFactory interface
    //---------------------------------------------------------------------
    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }
    @Override
    public int getBeanDefinitionCount() {
        return beanDefinitionMap.size();
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return getBeanNamesForType(type, true, true);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
            return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
        }
        Map<Class<?>, String[]> cache =
                (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
        String[] resolvedBeanNames = cache.get(type);
        if (resolvedBeanNames != null) {
            return resolvedBeanNames;
        }
        resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
        if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
            cache.put(type, resolvedBeanNames);
        }
        return resolvedBeanNames;
    }
    private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        List<String> result = new ArrayList<String>();

        // Check all bean definitions.
        for (String beanName : this.beanDefinitionNames) {
            // Only consider bean as eligible if the bean name
            // is not defined as alias for some other bean.
            if (!isAlias(beanName)) {
                try {
                    RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                    // Only check bean definition if it is complete.
                    if (!mbd.isAbstract() && (allowEagerInit ||
                            ((mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading())) &&
                                    !requiresEagerInitForType(mbd.getFactoryBeanName()))) {
                        // In case of FactoryBean, match object created by FactoryBean.
                        boolean isFactoryBean = isFactoryBean(beanName, mbd);
                        boolean matchFound = (allowEagerInit || !isFactoryBean || containsSingleton(beanName)) &&
                                (includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type);
                        if (!matchFound && isFactoryBean) {
                            // In case of FactoryBean, try to match FactoryBean instance itself next.
                            beanName = FACTORY_BEAN_PREFIX + beanName;
                            matchFound = (includeNonSingletons || mbd.isSingleton()) && isTypeMatch(beanName, type);
                        }
                        if (matchFound) {
                            result.add(beanName);
                        }
                    }
                }
                catch (CannotLoadBeanClassException ex) {
                    if (allowEagerInit) {
                        throw ex;
                    }
                    // Probably contains a placeholder: let's ignore it for type matching purposes.
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Ignoring bean class loading failure for bean '" + beanName + "'", ex);
                    }
                    onSuppressedException(ex);
                }
                catch (BeanDefinitionStoreException ex) {
                    if (allowEagerInit) {
                        throw ex;
                    }
                    // Probably contains a placeholder: let's ignore it for type matching purposes.
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Ignoring unresolvable metadata in bean definition '" + beanName + "'", ex);
                    }
                    onSuppressedException(ex);
                }
            }
        }

        // Check manually registered singletons too.
        for (String beanName : this.manualSingletonNames) {
            try {
                // In case of FactoryBean, match object created by FactoryBean.
                if (isFactoryBean(beanName)) {
                    if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
                        result.add(beanName);
                        // Match found for this bean: do not match FactoryBean itself anymore.
                        continue;
                    }
                    // In case of FactoryBean, try to match FactoryBean itself next.
                    beanName = FACTORY_BEAN_PREFIX + beanName;
                }
                // Match raw bean instance (might be raw FactoryBean).
                if (isTypeMatch(beanName, type)) {
                    result.add(beanName);
                }
            }
            catch (NoSuchBeanDefinitionException ex) {
                // Shouldn't happen - probably a result of circular reference resolution...
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to check manually registered singleton with name '" + beanName + "'", ex);
                }
            }
        }

        return StringUtils.toStringArray(result);
    }

    private boolean requiresEagerInitForType(String factoryBeanName) {
        return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
    }
    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        return getBeansOfType(type, true, true);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
            throws BeansException {

        String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        Map<String, T> result = new LinkedHashMap<String, T>(beanNames.length);
        for (String beanName : beanNames) {
            try {
                result.put(beanName, getBean(beanName, type));
            }
            catch (BeanCreationException ex) {
                Throwable rootCause = ex.getMostSpecificCause();
                if (rootCause instanceof BeanCurrentlyInCreationException) {
                    BeanCreationException bce = (BeanCreationException) rootCause;
                    if (isCurrentlyInCreation(bce.getBeanName())) {
                        if (this.logger.isDebugEnabled()) {
                            this.logger.debug("Ignoring match to currently created bean '" + beanName + "': " +
                                    ex.getMessage());
                        }
                        onSuppressedException(ex);
                        // Ignore: indicates a circular reference when autowiring constructors.
                        // We want to find matches other than the currently created bean itself.
                        continue;
                    }
                }
                throw ex;
            }
        }
        return result;
    }


    //---------------------------------------------------------------------
    // Implementation of ConfigurableListableBeanFactory interface
    //---------------------------------------------------------------------
    @Override
    public void registerResolvableDependency(Class<?> dependencyType, Object autowiredValue) {
        Assert.notNull(dependencyType, "Type must not be null");
        if (autowiredValue != null) {
            Assert.isTrue((autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue)),
                    "Value [" + autowiredValue + "] does not implement specified type [" + dependencyType.getName() + "]");
            this.resolvableDependencies.put(dependencyType, autowiredValue);
        }
    }
    @Override
    public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
            throws NoSuchBeanDefinitionException {

        return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
    }
    /**
     * Determine whether the specified bean definition qualifies as an autowire candidate,
     * to be injected into other beans which declare a dependency of matching type.
     * @param beanName the name of the bean definition to check
     * @param descriptor the descriptor of the dependency to resolve
     * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
     * @return whether the bean should be considered as autowire candidate
     */
    protected boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
            throws NoSuchBeanDefinitionException {

        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
        if (containsBeanDefinition(beanDefinitionName)) {
            return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(beanDefinitionName), descriptor, resolver);
        }
        else if (containsSingleton(beanName)) {
            return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
        }
        else if (getParentBeanFactory() instanceof DefaultListableBeanFactory) {
            // No bean definition found in this factory -> delegate to parent.
            return ((DefaultListableBeanFactory) getParentBeanFactory()).isAutowireCandidate(beanName, descriptor, resolver);
        }
        else if (getParentBeanFactory() instanceof ConfigurableListableBeanFactory) {
            // If no DefaultListableBeanFactory, can't pass the resolver along.
            return ((ConfigurableListableBeanFactory) getParentBeanFactory()).isAutowireCandidate(beanName, descriptor);
        }
        else {
            return true;
        }
    }

    /**
     * Determine whether the specified bean definition qualifies as an autowire candidate,
     * to be injected into other beans which declare a dependency of matching type.
     * @param beanName the name of the bean definition to check
     * @param mbd the merged bean definition to check
     * @param descriptor the descriptor of the dependency to resolve
     * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
     * @return whether the bean should be considered as autowire candidate
     */
    protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd,
                                          DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
        resolveBeanClass(mbd, beanDefinitionName);
        if (mbd.isFactoryMethodUnique) {
            boolean resolve;
            synchronized (mbd.constructorArgumentLock) {
                resolve = (mbd.resolvedConstructorOrFactoryMethod == null);
            }
            if (resolve) {
                new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
            }
        }
        return resolver.isAutowireCandidate(
                new BeanDefinitionHolder(mbd, beanName, getAliases(beanDefinitionName)), descriptor);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd == null) {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }
        return bd;
    }
    @Override
    public void clearMetadataCache() {
        super.clearMetadataCache();
        clearByTypeCache();
    }

    public boolean isConfigurationFrozen() {
        return this.configurationFrozen;
    }
    //---------------------------------------------------------------------
    // Implementation of BeanDefinitionRegistry interface
    //---------------------------------------------------------------------
    /*
     * 注册bean定义，需要给定唯一bean的名称和bean的定义,放到bean定义集合中
     */
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        Assert.hasText(beanName, "Bean name must not be empty");
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");

        if (beanDefinition instanceof AbstractBeanDefinition) {
            try {
                ((AbstractBeanDefinition) beanDefinition).validate();
            }
            catch (BeanDefinitionValidationException ex) {
                throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                        "Validation of bean definition failed", ex);
            }
        }
        BeanDefinition oldBeanDefinition;

        oldBeanDefinition = this.beanDefinitionMap.get(beanName);
        if (oldBeanDefinition != null) {
            if (!isAllowBeanDefinitionOverriding()) {
                throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                        "Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
                                "': There is already [" + oldBeanDefinition + "] bound.");
            }
            else if (oldBeanDefinition.getRole() < beanDefinition.getRole()) {
                // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
                if (this.logger.isWarnEnabled()) {
                    this.logger.warn("Overriding user-defined bean definition for bean '" + beanName +
                            "' with a framework-generated bean definition: replacing [" +
                            oldBeanDefinition + "] with [" + beanDefinition + "]");
                }
            }
            else if (!beanDefinition.equals(oldBeanDefinition)) {
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("Overriding bean definition for bean '" + beanName +
                            "' with a different definition: replacing [" + oldBeanDefinition +
                            "] with [" + beanDefinition + "]");
                }
            }
            else {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Overriding bean definition for bean '" + beanName +
                            "' with an equivalent definition: replacing [" + oldBeanDefinition +
                            "] with [" + beanDefinition + "]");
                }
            }
            this.beanDefinitionMap.put(beanName, beanDefinition);
        }
        else {
            if (hasBeanCreationStarted()) {
                // Cannot modify startup-time collection elements anymore (for stable iteration)
                synchronized (this.beanDefinitionMap) {
                    this.beanDefinitionMap.put(beanName, beanDefinition);
                    List<String> updatedDefinitions = new ArrayList<String>(this.beanDefinitionNames.size() + 1);
                    updatedDefinitions.addAll(this.beanDefinitionNames);
                    updatedDefinitions.add(beanName);
                    this.beanDefinitionNames = updatedDefinitions;
                    if (this.manualSingletonNames.contains(beanName)) {
                        Set<String> updatedSingletons = new LinkedHashSet<String>(this.manualSingletonNames);
                        updatedSingletons.remove(beanName);
                        this.manualSingletonNames = updatedSingletons;
                    }
                }
            }
            else {
                // Still in startup registration phase
                this.beanDefinitionMap.put(beanName, beanDefinition);
                this.beanDefinitionNames.add(beanName);
                this.manualSingletonNames.remove(beanName);
            }
            this.frozenBeanDefinitionNames = null;
        }

        if (oldBeanDefinition != null || containsSingleton(beanName)) {
            resetBeanDefinition(beanName);
        }

    }
    /**
     * Reset all bean definition caches for the given bean,
     * including the caches of beans that are derived from it.
     * @param beanName the name of the bean to reset
     */
    protected void resetBeanDefinition(String beanName) {
        // Remove the merged bean definition for the given bean, if already created.
        clearMergedBeanDefinition(beanName);

        // Remove corresponding bean from singleton cache, if any. Shouldn't usually
        // be necessary, rather just meant for overriding a context's default beans
        // (e.g. the default StaticMessageSource in a StaticApplicationContext).
        destroySingleton(beanName);

        // Reset all bean definitions that have the given bean as parent (recursively).
        for (String bdName : this.beanDefinitionNames) {
            if (!beanName.equals(bdName)) {
                BeanDefinition bd = this.beanDefinitionMap.get(bdName);
                if (beanName.equals(bd.getParentName())) {
                    resetBeanDefinition(bdName);
                }
            }
        }
    }

    /**
     * Remove any assumptions about by-type mappings.
     */
    private void clearByTypeCache() {
        this.allBeanNamesByType.clear();
        this.singletonBeanNamesByType.clear();
    }


    //---------------------------------------------------------------------
    // Dependency resolution functionality
    //---------------------------------------------------------------------

    /**
     * 获取依赖属性
     */
    @Override
    public Object resolveDependency(DependencyDescriptor descriptor, String beanName,
                                    Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException {
        descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
       /* if (descriptor.getDependencyType().equals(javaUtilOptionalClass)) {
            return new OptionalDependencyFactory().createOptionalDependency(descriptor, beanName);
        } else if (ObjectFactory.class == descriptor.getDependencyType()) {
            return new DependencyObjectFactory(descriptor, beanName);
        }*//* else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
            return new DependencyProviderFactory().createDependencyProvider(descriptor, beanName);
        }*//* else {
            Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(descriptor, beanName);
            if (result == null) {
                result = doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
            }
            return result;
        }*/
        Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(descriptor, beanName);
        if (result == null) {
            result = doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
        }
        return result;
    }

    public Object doResolveDependency(DependencyDescriptor descriptor, String beanName,
                                      Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException {
        // 注入属性的类型
        Class<?> type = descriptor.getDependencyType();
        // 处理@Value注解-------------------------------------
        // 获取@Value中的value属性
        Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
        if (value != null) {
            // 解析value
            if (value instanceof String) {
                String strVal = resolveEmbeddedValue((String) value);
                BeanDefinition bd = (beanName != null && containsBean(beanName) ? getMergedBeanDefinition(beanName) : null);
                value = evaluateBeanDefinitionString(strVal, bd);
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            // 如果需要会进行类型转换后返回结果
            return (descriptor.getField() != null ?
                    converter.convertIfNecessary(value, type, descriptor.getField()) :
                    converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
        }

        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            DependencyDescriptor targetDesc = new DependencyDescriptor(descriptor);
            targetDesc.increaseNestingLevel();
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType, targetDesc);
            if (matchingBeans.isEmpty()) {
                if (descriptor.isRequired()) {
                    raiseNoSuchBeanDefinitionException(componentType, "array of " + componentType.getName(), descriptor);
                }
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), type);
            if (getDependencyComparator() != null && result instanceof Object[]) {
                Arrays.sort((Object[]) result, adaptDependencyComparator(matchingBeans));
            }
            return result;
        }
        else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
            Class<?> elementType = descriptor.getCollectionType();
            if (elementType == null) {
                if (descriptor.isRequired()) {
                    throw new FatalBeanException("No element type declared for collection [" + type.getName() + "]");
                }
                return null;
            }
            DependencyDescriptor targetDesc = new DependencyDescriptor(descriptor);
            targetDesc.increaseNestingLevel();
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType, targetDesc);
            if (matchingBeans.isEmpty()) {
                if (descriptor.isRequired()) {
                    raiseNoSuchBeanDefinitionException(elementType, "collection of " + elementType.getName(), descriptor);
                }
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), type);
            if (getDependencyComparator() != null && result instanceof List) {
                Collections.sort((List<?>) result, adaptDependencyComparator(matchingBeans));
            }
            return result;
        }
        else if (Map.class.isAssignableFrom(type) && type.isInterface()) {
            Class<?> keyType = descriptor.getMapKeyType();
            if (String.class != keyType) {
                if (descriptor.isRequired()) {
                    throw new FatalBeanException("Key type [" + keyType + "] of map [" + type.getName() +
                            "] must be [java.lang.String]");
                }
                return null;
            }
            Class<?> valueType = descriptor.getMapValueType();
            if (valueType == null) {
                if (descriptor.isRequired()) {
                    throw new FatalBeanException("No value type declared for map [" + type.getName() + "]");
                }
                return null;
            }
            DependencyDescriptor targetDesc = new DependencyDescriptor(descriptor);
            targetDesc.increaseNestingLevel();
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType, targetDesc);
            if (matchingBeans.isEmpty()) {
                if (descriptor.isRequired()) {
                    raiseNoSuchBeanDefinitionException(valueType, "map with value type " + valueType.getName(), descriptor);
                }
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            return matchingBeans;
        }
        else {
            // 对非数组、容器对象的处理
            // 获取所有类型匹配的Map(beanName->bean实例)
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
            if (matchingBeans.isEmpty()) {
                if (descriptor.isRequired()) {
                    // 如果配置了required属性为true(默认值也是true)的话,抛出异常
                    raiseNoSuchBeanDefinitionException(type, "", descriptor);
                }
                return null;
            }
            // 符合条件的bean不止一个
            if (matchingBeans.size() > 1) {
                String primaryBeanName = determineAutowireCandidate(matchingBeans, descriptor);
                if (primaryBeanName == null) {
                    throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
                }
                if (autowiredBeanNames != null) {
                    autowiredBeanNames.add(primaryBeanName);
                }
                return matchingBeans.get(primaryBeanName);
            }
            // We have exactly one match.
            // 只有一个bean与类型匹配,那么直接使用该bean
            Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
            if (autowiredBeanNames != null) {
                autowiredBeanNames.add(entry.getKey());
            }
            return entry.getValue();
        }
    }

    private Comparator<Object> adaptDependencyComparator(Map<String, Object> matchingBeans) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).withSourceProvider(
                    createFactoryAwareOrderSourceProvider(matchingBeans));
        }
        else {
            return comparator;
        }
    }

    private FactoryAwareOrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, Object> beans) {
        IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<Object, String>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            instancesToBeanNames.put(entry.getValue(), entry.getKey());
        }
        return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
    }

    /**
     * 查找与所需类型匹配的bean实例,在为指定bean自动注入期间调用
     *
     * @param bean         name即将连接的bean的名称
     * @param requiredtype 要查找的实际bean类型 可以是数组组件类型或集合元素类型）
     * @param descriptor   要解析的依赖项的描述符
     * @return 匹配的候选名称和候选实例的映射
     */
    protected Map<String, Object> findAutowireCandidates(
            String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
        //获 取类型匹配的bean的beanName列表
        String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                this, requiredType, true, descriptor.isEager());

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        // 如果查找的类型是特殊类型或其子类的话,也将保存好的特殊类型的实例放到结果集中
        for (Class<?> autowiringType : this.resolvableDependencies.keySet()) {
            if (autowiringType.isAssignableFrom(requiredType)) {
                Object autowiringValue = this.resolvableDependencies.get(autowiringType);
                autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
                if (requiredType.isInstance(autowiringValue)) {
                    result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
                    break;
                }
            }
        }
        for (String candidateName : candidateNames) {
            // 不是自引用 && 符合注入条件
            if (!isSelfReference(beanName, candidateName) && isAutowireCandidate(candidateName, descriptor)) {
                result.put(candidateName, getBean(candidateName));
            }
        }
        //结果集为空 && 注入属性是非数组、容器类型
        //如果条件满足,Spring会放宽注入条件的限制
        if (result.isEmpty()) {
            DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
            // 如果结果还是为空,Spring会将自引用添加到结果中
            for (String candidateName : candidateNames) {
                if (!candidateName.equals(beanName) && isAutowireCandidate(candidateName, fallbackDescriptor)) {
                    result.put(candidateName, getBean(candidateName));
                }
            }
        }
        return result;
    }

    /**
     * 在为指定bean自动注入期间调用
     * 符合依赖条件的bean不止一个,从中多个bean中选出一个自动装配
     */
    protected String determineAutowireCandidate(Map<String, Object> candidateBeans, DependencyDescriptor descriptor) {
        Class<?> requiredType = descriptor.getDependencyType();
        /*String primaryCandidate = determinePrimaryCandidate(candidateBeans, requiredType);
        if (primaryCandidate != null) {
            return primaryCandidate;
        }*/
        String priorityCandidate = determineHighestPriorityCandidate(candidateBeans, requiredType);
        if (priorityCandidate != null) {
            return priorityCandidate;
        }
        // Fallback
        for (Map.Entry<String, Object> entry : candidateBeans.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) ||
                    matchesBeanName(candidateBeanName, descriptor.getDependencyName())) {
                return candidateBeanName;
            }
        }
        return null;
    }

    /**
     * Determine the primary candidate in the given set of beans.
     * @param candidateBeans a Map of candidate names and candidate instances
     * that match the required type
     * @param requiredType the target dependency type to match against
     * @return the name of the primary candidate, or {@code null} if none found
     * @see #isPrimary(String, Object)
     */
    protected String determinePrimaryCandidate(Map<String, Object> candidateBeans, Class<?> requiredType) {
        String primaryBeanName = null;
        for (Map.Entry<String, Object> entry : candidateBeans.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (isPrimary(candidateBeanName, beanInstance)) {
                if (primaryBeanName != null) {
                    boolean candidateLocal = containsBeanDefinition(candidateBeanName);
                    boolean primaryLocal = containsBeanDefinition(primaryBeanName);
                    if (candidateLocal && primaryLocal) {
                        throw new NoUniqueBeanDefinitionException(requiredType, candidateBeans.size(),
                                "more than one 'primary' bean found among candidates: " + candidateBeans.keySet());
                    }
                    else if (candidateLocal) {
                        primaryBeanName = candidateBeanName;
                    }
                }
                else {
                    primaryBeanName = candidateBeanName;
                }
            }
        }
        return primaryBeanName;
    }

    /**
     * 在为指定bean自动注入期间调用
     * 符合依赖条件的bean不止一个,确定优先级较高的bean
     */
    protected String determineHighestPriorityCandidate(Map<String, Object> candidateBeans, Class<?> requiredType) {
        String highestPriorityBeanName = null;
        Integer highestPriority = null;
        for (Map.Entry<String, Object> entry : candidateBeans.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            Integer candidatePriority = getPriority(beanInstance);
            if (candidatePriority != null) {
                if (highestPriorityBeanName != null) {
                    if (candidatePriority.equals(highestPriority)) {
                        throw new NoUniqueBeanDefinitionException(requiredType, candidateBeans.size(),
                                "Multiple beans found with the same priority ('" + highestPriority + "') " +
                                        "among candidates: " + candidateBeans.keySet());
                    } else if (candidatePriority < highestPriority) {
                        highestPriorityBeanName = candidateBeanName;
                        highestPriority = candidatePriority;
                    }
                } else {
                    highestPriorityBeanName = candidateBeanName;
                    highestPriority = candidatePriority;
                }
            }
        }
        return highestPriorityBeanName;
    }

    protected boolean isPrimary(String beanName, Object beanInstance) {
        if (containsBeanDefinition(beanName)) {
            return getMergedLocalBeanDefinition(beanName).isPrimary();
        }
        BeanFactory parentFactory = getParentBeanFactory();
        return (parentFactory instanceof DefaultListableBeanFactory &&
                ((DefaultListableBeanFactory) parentFactory).isPrimary(beanName, beanInstance));
    }

    /**
     * 在为指定bean自动注入期间调用,获取bean的优先级
     */
    protected Integer getPriority(Object beanInstance) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).getPriority(beanInstance);
        }
        return null;
    }

    protected boolean matchesBeanName(String beanName, String candidateName) {
        return (candidateName != null && (candidateName.equals(beanName)));
    }
    /**
     * Determine whether the given beanName/candidateName pair indicates a self reference,
     * i.e. whether the candidate points back to the original bean or to a factory method
     * on the original bean.
     */
    private boolean isSelfReference(String beanName, String candidateName) {
        return (beanName != null && candidateName != null &&
                (beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
                        beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
    }

    /**
     * Raise a NoSuchBeanDefinitionException for an unresolvable dependency.
     */
    private void raiseNoSuchBeanDefinitionException(
            Class<?> type, String dependencyDescription, DependencyDescriptor descriptor)
            throws NoSuchBeanDefinitionException {

        throw new NoSuchBeanDefinitionException(type, dependencyDescription,
                "expected at least 1 bean which qualifies as autowire candidate for this dependency. " +
                        "Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
    }



    /**
     * An {@link org.springframework.core.OrderComparator.OrderSourceProvider} implementation
     * that is aware of the bean metadata of the instances to sort.
     * <p>Lookup for the method factory of an instance to sort, if any, and let the
     * comparator retrieve the {@link org.springframework.core.annotation.Order}
     * value defined on it. This essentially allows for the following construct:
     */
    private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

        private final Map<Object, String> instancesToBeanNames;

        public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
            this.instancesToBeanNames = instancesToBeanNames;
        }

        @Override
        public Object getOrderSource(Object obj) {
            RootBeanDefinition beanDefinition = getRootBeanDefinition(this.instancesToBeanNames.get(obj));
            if (beanDefinition == null) {
                return null;
            }
            List<Object> sources = new ArrayList<Object>();
            Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
            if (factoryMethod != null) {
                sources.add(factoryMethod);
            }
            Class<?> targetType = beanDefinition.getTargetType();
            if (targetType != null && !targetType.equals(obj.getClass())) {
                sources.add(targetType);
            }
            return sources.toArray(new Object[sources.size()]);
        }

        private RootBeanDefinition getRootBeanDefinition(String beanName) {
            if (beanName != null && containsBeanDefinition(beanName)) {
                BeanDefinition bd = getMergedBeanDefinition(beanName);
                if (bd instanceof RootBeanDefinition) {
                    return (RootBeanDefinition) bd;
                }
            }
            return null;
        }
    }

}
