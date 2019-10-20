package com.zhaojuan.spring.beans.factory.support;

import com.zhaojuan.spring.beans.*;
import com.zhaojuan.spring.beans.config.BeanDefinition;
import com.zhaojuan.spring.core.util.BeanUtils;
import com.zhaojuan.spring.core.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.util.*;

/**
 * 自动装配以及属性设置
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {
    /**
     * 属性填充
     */
    public void populateBean(String beanName, BeanDefinition mbd, BeanWrapper bw) {
        PropertyValues pvs = mbd.getPropertyValues();

        if (mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_NAME ||
                mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_TYPE) {
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

            // Add property values based on autowire by name if applicable.
            if (mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }

            // Add property values based on autowire by type if applicable.
            if (mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }

            pvs = newPvs;
        }

        // xml中配置的property注入在这里进行装配,注解注入不在pvs中,它是通过上面processor注入的
        applyPropertyValues(beanName, mbd, bw, pvs);
    }

    /**
     * 根据名称自动装配
     */
    protected void autowireByName(String beanName, BeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

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
     * 根据Type自动装配
     */
    protected void autowireByType(String beanName, BeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        Set<String> autowiredBeanNames = new LinkedHashSet<String>(4);
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            // Don't try autowiring by type for type Object: never makes sense,
            // even if it technically is a unsatisfied, non-simple property.
            if (Object.class != pd.getPropertyType()) {
                MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                // Do not allow eager init for type matching in case of a prioritized post-processor.
                boolean eager = !PriorityOrdered.class.isAssignableFrom(bw.getWrappedClass());
                DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                if (autowiredArgument != null) {
                    pvs.add(propertyName, autowiredArgument);
                }
                for (String autowiredBeanName : autowiredBeanNames) {
                    registerDependentBean(autowiredBeanName, beanName);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Autowiring by type from bean name '" + beanName + "' via property '" +
                                propertyName + "' to bean named '" + autowiredBeanName + "'");
                    }
                }
                autowiredBeanNames.clear();
            }
        }
    }

    protected String[] unsatisfiedNonSimpleProperties(BeanDefinition mbd, BeanWrapper bw) {
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

    /**
     * 对象的属性赋值
     */
    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        List<PropertyValue> original;
        if (pvs instanceof MutablePropertyValues) {
            original = pvs.getPropertyValueList();
        } else {
            original = Arrays.asList(pvs.getPropertyValues());
        }
        // 创建深度副本,解析值的任何引用
        List<PropertyValue> deepCopy = new ArrayList<PropertyValue>(original.size());
        for (PropertyValue pv : original) {
            deepCopy.add(pv);
        }

        // 设置我们的深度副本
        try {
            bw.setPropertyValues(new MutablePropertyValues(deepCopy));
        } catch (Exception ex) {
            throw new BeansException(beanName + "Error setting property values" + ex);
        }

    }
}
