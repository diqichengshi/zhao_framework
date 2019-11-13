package org.springframework.beans.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 它间接实现 InstantiationAwareBeanPostProcessor，就具备了实例化前后 (而不是初始化前后) 管理对象的能力，
 * 实现了 BeanPostProcessor，具有初始化前后管理对象的能力，
 * 实现 BeanFactoryAware，具备随时拿到 BeanFactory 的能力，
 * 也就是说，这个 AutowiredAnnotationBeanPostProcessor 具备一切后置处理器的能力。
 */
public class AutowiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements MergedBeanDefinitionPostProcessor, BeanFactoryAware {

    protected final Log logger = LogFactory.getLog(getClass());

    private final Map<String, InjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<String, InjectionMetadata>(64);

    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes =
            new LinkedHashSet<Class<? extends Annotation>>();

    private String requiredParameterName = "required";

    private boolean requiredParameterValue = true;

    private DefaultListableBeanFactory beanFactory;

    public AutowiredAnnotationBeanPostProcessor() {
        this.autowiredAnnotationTypes.add(Autowired.class);
        this.autowiredAnnotationTypes.add(Value.class);
        try {
            this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
                    ClassUtils.forName("javax.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
            logger.info("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
        } catch (ClassNotFoundException ex) {
            // JSR-330 API not available - simply skip.
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof DefaultListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "AutowiredAnnotationBeanPostProcessor requires a DefaultListableBeanFactory");
        }

        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    /**
     * 并且把名字叫做beanName的类中的变量封装到InjectionMetadata中
     */
    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType != null) {
            // TODO 搜索每个Bean内@Autowired注解的信息
            InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
            metadata.checkConfigMembers(beanDefinition);
        }
    }

    /**
     * 首先找到需要注入的哪些元数据
     */
    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        // TODO Spring IOC容器从找到合适的Bean,注入属性
        // 首先找到需要注入的哪些元数据,然后metadata.inject(注入),注入方法点进去,来到InjectionMetadata的inject方法，
        // 在一个 for循环里面依次执行element.inject(target, beanName, pvs),来对属性进行注入。
        InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
        }
        return pvs;
    }

    /**
     * 首先找到需要注入的哪些元数据
     */
    private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    try {
                        // 解析@Autowired注解的信息,生成元数据,缓存起来
                        metadata = buildAutowiringMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to introspect bean class [" + clazz.getName() +
                                "] for autowiring metadata: could not find class that it depends on", err);
                    }
                }
            }
        }
        return metadata;
    }

    private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
        LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
        Class<?> targetClass = clazz;

        do {
            final LinkedList<InjectionMetadata.InjectedElement> currElements =
                    new LinkedList<InjectionMetadata.InjectedElement>();

            ReflectionUtils.doWithLocalFields(targetClass, new ReflectionUtils.FieldCallback() {
                @Override
                public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                    // 如果类内的属性上有@Autowired注解,则用工具类获取注解信息
                    AnnotationAttributes ann = findAutowiredAnnotation(field);
                    if (ann != null) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Autowired annotation is not supported on static fields: " + field);
                            }
                            return;
                        }
                        //获取@Autowired注解的required的属性值,如果true,但注入失败会抛出异常,false则不会
                        boolean required = determineRequiredStatus(ann);
                        currElements.add(new AutowiredFieldElement(field, required));
                    }
                }
            });

            ReflectionUtils.doWithLocalMethods(targetClass, new ReflectionUtils.MethodCallback() {
                @Override
                public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                    Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
                    if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                        return;
                    }
                    // 如果方法上有@Autowired注解,则获取注解信息
                    AnnotationAttributes ann = findAutowiredAnnotation(bridgedMethod);
                    if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
                        // @Autowired不支持静态方法
                        if (Modifier.isStatic(method.getModifiers())) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Autowired annotation is not supported on static methods: " + method);
                            }
                            return;
                        }
                        // @Autowired注解标识在方法上的目的就是将容器内的Bean注入到方法的参数中,没有参数就违背了初衷
                        if (method.getParameterTypes().length == 0) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Autowired annotation should be used on methods with parameters: " + method);
                            }
                        }
                        boolean required = determineRequiredStatus(ann);
                        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                        currElements.add(new AutowiredMethodElement(method, required, pd));
                    }
                }
            });

            elements.addAll(0, currElements);
            // 在解析完Bean的类型之后,递归的解析其父类,将所有的@Autowired的属性和方法收集起来,
            // 且类的层级越高其属性会被越优先注入
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);

        return new InjectionMetadata(clazz, elements);
    }


    private AnnotationAttributes findAutowiredAnnotation(AccessibleObject ao) {
        for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
            AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ao, type);
            if (attributes != null) {
                return attributes;
            }
        }
        return null;
    }

    /**
     * Determine if the annotated field or method requires its dependency.
     * <p>A 'required' dependency means that autowiring should fail when no beans
     * are found. Otherwise, the autowiring process will simply bypass the field
     * or method when no beans are found.
     *
     * @param ann the Autowired annotation
     * @return whether the annotation indicates that a dependency is required
     */
    protected boolean determineRequiredStatus(AnnotationAttributes ann) {
        return (!ann.containsKey(this.requiredParameterName) ||
                this.requiredParameterValue == ann.getBoolean(this.requiredParameterName));
    }

    /**
     * Register the specified bean as dependent on the autowired beans.
     */
    private void registerDependentBeans(String beanName, Set<String> autowiredBeanNames) {
        if (beanName != null) {
            for (String autowiredBeanName : autowiredBeanNames) {
                if (this.beanFactory.containsBean(autowiredBeanName)) {
                    this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Autowiring by type from bean name '" + beanName +
                            "' to bean named '" + autowiredBeanName + "'");
                }
            }
        }
    }

    /**
     * Resolve the specified cached method argument or field value.
     */
    private Object resolvedCachedArgument(String beanName, Object cachedArgument) {
        if (cachedArgument instanceof DependencyDescriptor) {
            DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
            return this.beanFactory.resolveDependency(descriptor, beanName, null,null);
        }
        /*else if (cachedArgument instanceof RuntimeBeanReference) {
            return this.beanFactory.getBean(((RuntimeBeanReference) cachedArgument).getBeanName());
        }*/
        else {
            return cachedArgument;
        }
    }

    //==========================================================================
    //============================内部类定义=====================================
    //==========================================================================

    /**
     * Class representing injection information about an annotated field.
     */
    private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

        private final boolean required;

        private volatile boolean cached = false;

        private volatile Object cachedFieldValue;

        public AutowiredFieldElement(Field field, boolean required) {
            super(field, null);
            this.required = required;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            Field field = (Field) this.member;
            try {
                Object value;
                if (this.cached) {
                    value = resolvedCachedArgument(beanName, this.cachedFieldValue);
                } else {
                    DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
                    desc.setContainingClass(bean.getClass());
                    Set<String> autowiredBeanNames = new LinkedHashSet<String>(1);
                    TypeConverter typeConverter = beanFactory.getTypeConverter();
                    value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames,typeConverter);
                    synchronized (this) {
                        if (!this.cached) {
                            if (value != null || this.required) {
                                this.cachedFieldValue = desc;
                                registerDependentBeans(beanName, autowiredBeanNames);
                                if (autowiredBeanNames.size() == 1) {
                                    String autowiredBeanName = autowiredBeanNames.iterator().next();
                                    if (beanFactory.containsBean(autowiredBeanName)) {
                                        if (beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                                            this.cachedFieldValue = new RuntimeBeanReference(autowiredBeanName);
                                        }
                                    }
                                }
                            } else {
                                this.cachedFieldValue = null;
                            }
                            this.cached = true;
                        }
                    }
                }
                if (value != null) {
                    ReflectionUtils.makeAccessible(field);
                    field.set(bean, value);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                throw new BeanCreationException("Could not autowire field: " + field, ex);
            }
        }
    }


    /**
     * Class representing injection information about an annotated method.
     */
    private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

        private final boolean required;

        private volatile boolean cached = false;

        private volatile Object[] cachedMethodArguments;

        public AutowiredMethodElement(Method method, boolean required, PropertyDescriptor pd) {
            super(method, pd);
            this.required = required;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            if (checkPropertySkipping(pvs)) {
                return;
            }
            Method method = (Method) this.member;
            try {
                Object[] arguments;
                if (this.cached) {
                    // Shortcut for avoiding synchronization...
                    arguments = resolveCachedArguments(beanName);
                } else {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    arguments = new Object[paramTypes.length];
                    DependencyDescriptor[] descriptors = new DependencyDescriptor[paramTypes.length];
                    Set<String> autowiredBeanNames = new LinkedHashSet<String>(paramTypes.length);
                    TypeConverter typeConverter = beanFactory.getTypeConverter();
                    for (int i = 0; i < arguments.length; i++) {
                        MethodParameter methodParam = new MethodParameter(method, i);
                        DependencyDescriptor desc = new DependencyDescriptor(methodParam, this.required);
                        desc.setContainingClass(bean.getClass());
                        descriptors[i] = desc;
                        Object arg = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames,typeConverter);
                        if (arg == null && !this.required) {
                            arguments = null;
                            break;
                        }
                        arguments[i] = arg;
                    }
                    synchronized (this) {
                        if (!this.cached) {
                            if (arguments != null) {
                                this.cachedMethodArguments = new Object[arguments.length];
                                for (int i = 0; i < arguments.length; i++) {
                                    this.cachedMethodArguments[i] = descriptors[i];
                                }
                                registerDependentBeans(beanName, autowiredBeanNames);
                                if (autowiredBeanNames.size() == paramTypes.length) {
                                    Iterator<String> it = autowiredBeanNames.iterator();
                                    for (int i = 0; i < paramTypes.length; i++) {
                                        String autowiredBeanName = it.next();
                                        if (beanFactory.containsBean(autowiredBeanName)) {
                                            if (beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
                                                this.cachedMethodArguments[i] = new RuntimeBeanReference(autowiredBeanName);
                                            }
                                        }
                                    }
                                }
                            } else {
                                this.cachedMethodArguments = null;
                            }
                            this.cached = true;
                        }
                    }
                }
                if (arguments != null) {
                    ReflectionUtils.makeAccessible(method);
                    method.invoke(bean, arguments);
                }
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            } catch (Throwable ex) {
                throw new BeanCreationException("Could not autowire method: " + method, ex);
            }
        }

        private Object[] resolveCachedArguments(String beanName) {
            if (this.cachedMethodArguments == null) {
                return null;
            }
            Object[] arguments = new Object[this.cachedMethodArguments.length];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = resolvedCachedArgument(beanName, this.cachedMethodArguments[i]);
            }
            return arguments;
        }
    }

}
