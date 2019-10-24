package org.springframework.beans.factory.support;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.*;
import org.springframework.beans.exception.BeanCreationException;
import org.springframework.core.MethodParameter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 自动装配以及属性设置
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {

    /**
     * 实现抽象类的方法
     */
    protected Object createBean(final String beanName, final BeanDefinition mbd, final Object[] args) {
        return doCreateBean(beanName, mbd, args);
    }

    protected Object doCreateBean(final String beanName, final BeanDefinition mbd, final Object[] args) {
        // Instantiate the bean.
        BeanWrapper instanceWrapper = null;
        if (instanceWrapper == null) {
            //这个是主要方法一，生成wrapper下面分析
            instanceWrapper = createBeanInstance(beanName, mbd);
        }
        //获取bean和class对象
        final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
        Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);
        // Initialize the bean instance.
        Object exposedObject = bean;
        try {
            //属性填充，重点方法二
            populateBean(beanName, mbd, instanceWrapper);
            if (exposedObject != null) {
                //实现InitializingBean接口的方法回调，重点方法三
                exposedObject = initializeBean(beanName, exposedObject, mbd);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (ex instanceof BeansException) {
                throw (BeansException) ex;
            } else {
                throw new BeansException(beanName + "Initialization of bean failed", ex);
            }
        }
        return exposedObject;
    }

    private BeanWrapper createBeanInstance(String beanName, BeanDefinition bd) {
        // Make sure bean class is actually resolved at this point.
        Class<?> beanClass = bd.getBeanClass();

        if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) ) {
            throw new BeanCreationException(beanName
                    +"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
        }

        return instantiateBean(beanName, bd);
    }

    /**
     * 属性填充
     */
    public void populateBean(String beanName, BeanDefinition mbd, BeanWrapper bw) {
        PropertyValues pvs = mbd.getPropertyValues();

        if (mbd.getResolvedAutowireMode() == GenericBeanDefinition.AUTOWIRE_BY_NAME||
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
        // xml中配置的property注入在这里进行装配,注解注入不在pvs中,它是通过上面processor注入的
        applyPropertyValues(beanName, mbd, bw, pvs);
    }

    /**
     * 根据名称自动装配
     */
    protected void autowireByName(String beanName, BeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
        // 解析出非简单属性
        String[] propertyNames = mbd.getDependsOn();
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
    protected void autowireByType(String beanName, BeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
    }

    /**
     * 对象的属性赋值
     */
    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        if (pvs == null || pvs.isEmpty()) {
            return;
        }

        MutablePropertyValues mpvs = (MutablePropertyValues) pvs;

        if (mpvs.isConverted()) {
            try {
                bw.setPropertyValues(mpvs);
                return;
            } catch (BeansException ex) {
                throw new BeanCreationException(beanName+ "Error setting property values", ex);
            }
        }

        List<PropertyValue> original = mpvs.getPropertyValueList(); // 获取bean的属性列表

        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd);

        // Create a deep copy, resolving any references for values.
        List<PropertyValue> deepCopy = new ArrayList<PropertyValue>(original.size());
        boolean resolveNecessary = false;
        for (PropertyValue pv : original) {
            if (pv.isConverted()) {
                deepCopy.add(pv);
            }
            else {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
                Object convertedValue = resolvedValue;
                boolean convertible = !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
                if (convertible) {
                    convertedValue=((BeanWrapperImpl) bw).convertForProperty(propertyName,resolvedValue); // 进行属性转换 TODO 重要方法
                }
                // Possibly store converted value in merged bean definition,
                // in order to avoid re-conversion for every created bean instance.
                if (resolvedValue == originalValue) {
                    if (convertible) {
                        pv.setConvertedValue(convertedValue);
                    }
                    deepCopy.add(pv);
                }
                else if (convertible  &&
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
        }
        catch (BeansException ex) {
            throw new BeanCreationException(beanName+"Error setting property values", ex);
        }
    }

}
