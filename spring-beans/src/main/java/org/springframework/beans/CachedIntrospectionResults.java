package org.springframework.beans;

import org.springframework.beans.exception.FatalBeanException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 缓存内省结果
 * 缓存java beans的内部类
 * Java类的信息。不打算由应用程序代码直接使用
 */
public class CachedIntrospectionResults {
    private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);

    private final Map<String, PropertyDescriptor> propertyDescriptorCache;

    private static List<BeanInfoFactory> beanInfoFactories = SpringFactoriesLoader.loadFactories(
            BeanInfoFactory.class, CachedIntrospectionResults.class.getClassLoader());

    private final BeanInfo beanInfo;


    static final Set<ClassLoader> acceptedClassLoaders =
            Collections.newSetFromMap(new ConcurrentHashMap<ClassLoader, Boolean>(16));


    static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache =
            new ConcurrentHashMap<Class<?>, CachedIntrospectionResults>(64);

    static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache =
            new ConcurrentReferenceHashMap<Class<?>, CachedIntrospectionResults>(64);

    private final ConcurrentMap<PropertyDescriptor, TypeDescriptor> typeDescriptorCache;


    /**
     * Create a new CachedIntrospectionResults instance for the given class.
     */
    private CachedIntrospectionResults(Class<?> beanClass) throws BeansException {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting BeanInfo for class [" + beanClass.getName() + "]");
            }

            BeanInfo beanInfo = null;
            for (BeanInfoFactory beanInfoFactory : beanInfoFactories) {
                beanInfo = beanInfoFactory.getBeanInfo(beanClass);
                if (beanInfo != null) {
                    break;
                }
            }
            if (beanInfo == null) {
                // If none of the factories supported the class, fall back to the default
                beanInfo = Introspector.getBeanInfo(beanClass);
            }
            this.beanInfo = beanInfo;

            if (logger.isTraceEnabled()) {
                logger.trace("Caching PropertyDescriptors for class [" + beanClass.getName() + "]");
            }
            this.propertyDescriptorCache = new LinkedHashMap<String, PropertyDescriptor>();

            // This call is slow so we do it once.
            PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (Class.class == beanClass &&
                        ("classLoader".equals(pd.getName()) || "protectionDomain".equals(pd.getName()))) {
                    // Ignore Class.getClassLoader() and getProtectionDomain() methods - nobody needs to bind to those
                    continue;
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Found bean property '" + pd.getName() + "'" +
                            (pd.getPropertyType() != null ? " of type [" + pd.getPropertyType().getName() + "]" : "") +
                            (pd.getPropertyEditorClass() != null ?
                                    "; editor [" + pd.getPropertyEditorClass().getName() + "]" : ""));
                }
                pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
                this.propertyDescriptorCache.put(pd.getName(), pd);
            }

            this.typeDescriptorCache = new ConcurrentReferenceHashMap<PropertyDescriptor, TypeDescriptor>();
        } catch (IntrospectionException ex) {
            throw new FatalBeanException("Failed to obtain BeanInfo for class [" + beanClass.getName() + "]", ex);
        }
    }


    /**
     * Create CachedIntrospectionResults for the given bean class.
     */
    @SuppressWarnings("unchecked")
    static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
        CachedIntrospectionResults results = strongClassCache.get(beanClass);
        if (results != null) {
            return results;
        }
        results = softClassCache.get(beanClass);
        if (results != null) {
            return results;
        }

        results = new CachedIntrospectionResults(beanClass);
        ConcurrentMap<Class<?>, CachedIntrospectionResults> classCacheToUse = softClassCache;

        CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
        return (existing != null ? existing : results);
    }

    PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] pds = new PropertyDescriptor[this.propertyDescriptorCache.size()];
        int i = 0;
        for (PropertyDescriptor pd : this.propertyDescriptorCache.values()) {
            pds[i] = (pd instanceof GenericTypeAwarePropertyDescriptor ? pd :
                    buildGenericTypeAwarePropertyDescriptor(getBeanClass(), pd));
            i++;
        }
        return pds;
    }

    Class<?> getBeanClass() {
        return this.beanInfo.getBeanDescriptor().getBeanClass();
    }

    PropertyDescriptor getPropertyDescriptor(String name) {
        PropertyDescriptor pd = this.propertyDescriptorCache.get(name);
        if (pd == null && StringUtils.hasLength(name)) {
            // Same lenient fallback checking as in PropertyTypeDescriptor...
            pd = this.propertyDescriptorCache.get(name.substring(0, 1).toLowerCase() + name.substring(1));
            if (pd == null) {
                pd = this.propertyDescriptorCache.get(name.substring(0, 1).toUpperCase() + name.substring(1));
            }
        }
        return (pd == null || pd instanceof GenericTypeAwarePropertyDescriptor ? pd :
                buildGenericTypeAwarePropertyDescriptor(getBeanClass(), pd));
    }

    private PropertyDescriptor buildGenericTypeAwarePropertyDescriptor(Class<?> beanClass, PropertyDescriptor pd) {
        try {
            return new GenericTypeAwarePropertyDescriptor(beanClass, pd.getName(), pd.getReadMethod(),
                    pd.getWriteMethod(), pd.getPropertyEditorClass());
        } catch (IntrospectionException ex) {
            throw new FatalBeanException("Failed to re-introspect class [" + beanClass.getName() + "]", ex);
        }
    }

    TypeDescriptor addTypeDescriptor(PropertyDescriptor pd, TypeDescriptor td) {
        TypeDescriptor existing = this.typeDescriptorCache.putIfAbsent(pd, td);
        return (existing != null ? existing : td);
    }

    TypeDescriptor getTypeDescriptor(PropertyDescriptor pd) {
        return this.typeDescriptorCache.get(pd);
    }
}
