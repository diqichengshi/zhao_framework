package org.springframework.beans.factory.support;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.BeansException;
import org.springframework.beans.exception.NoSuchBeanDefinitionException;
import org.springframework.beans.exception.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.OrderComparator;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();

    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<Class<?>, Object>(16);

    private Comparator<Object> dependencyComparator;

    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public DefaultListableBeanFactory() {
        super();
    }

    public DefaultListableBeanFactory(BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    /*
     * 注册bean定义，需要给定唯一bean的名称和bean的定义,放到bean定义集合中
     */
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        Objects.requireNonNull(beanName, "beanName不能为空");
        Objects.requireNonNull(beanDefinition, "beanDefinition不能为空");
        if (beanDefinitionMap.containsKey(beanName)) {
            throw new BeanDefinitionStoreException("已存在【" + beanName + "】的bean定义" + getBeanDefinition(beanName));
        }
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    /**
     * 获取依赖属性
     */
    @Override
    public Object resolveDependency(DependencyDescriptor descriptor, String beanName,
                                    Set<String> autowiredBeanNames, TypeConverter converter) throws BeansException {
        Class<?> type = descriptor.getDependencyType();
        Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
        if (matchingBeans.isEmpty()) {
            if (descriptor.isRequired()) {
                throw new NoSuchBeanDefinitionException(type,
                        "expected at least 1 bean which qualifies as autowire candidate for this dependency. " +
                                "Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
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
        Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
        if (autowiredBeanNames != null) {
            autowiredBeanNames.add(entry.getKey());
        }
        return entry.getValue();
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

        Map<String, Object> result = new LinkedHashMap<String, Object>();
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

    public Comparator<Object> getDependencyComparator() {
        return this.dependencyComparator;
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitionMap.get(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

    public int getBeanDefinitionCount() {
        return beanDefinitionMap.size();
    }
}
