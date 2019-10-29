package org.springframework.beans.factory.support;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.exception.BeanDefinitionStoreException;
import org.springframework.beans.exception.BeansException;
import org.springframework.beans.exception.NoSuchBeanDefinitionException;
import org.springframework.beans.exception.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.OrderComparator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory{

    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();

    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<Class<?>, Object>(16);

    private Comparator<Object> dependencyComparator;

    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public DefaultListableBeanFactory() {
        super();
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

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitionMap.get(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }


    /**
     * 获取依赖属性
     */
    public Object resolveDependency(DependencyDescriptor descriptor, String beanName,
                                    Set<String> autowiredBeanNames) throws BeansException {
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
     * Find bean instances that match the required type.
     * Called during autowiring for the specified bean.
     * @param beanName the name of the bean that is about to be wired
     * @param requiredType the actual type of bean to look for
     * (may be an array component type or collection element type)
     * @param descriptor the descriptor of the dependency to resolve
     * @return a Map of candidate names and candidate instances that match
     * the required type (never {@code null})
     * @throws BeansException in case of errors
     * @see #autowireByType
     * @see #autowireConstructor
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
     * Determine the autowire candidate in the given set of beans.
     * <p>Looks for {@code @Primary} and {@code @Priority} (in that order).
     * @param candidateBeans a Map of candidate names and candidate instances
     * that match the required type, as returned by {@link #findAutowireCandidates}
     * @param descriptor the target dependency to match against
     * @return the name of the autowire candidate, or {@code null} if none found
     */
    protected String determineAutowireCandidate(Map<String, Object> candidateBeans, DependencyDescriptor descriptor) {
        Class<?> requiredType = descriptor.getDependencyType();
        String primaryCandidate = determinePrimaryCandidate(candidateBeans, requiredType);
        if (primaryCandidate != null) {
            return primaryCandidate;
        }
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
     * Determine the candidate with the highest priority in the given set of beans. As
     * defined by the {@link org.springframework.core.Ordered} interface, the lowest
     * value has the highest priority.
     * @param candidateBeans a Map of candidate names and candidate instances
     * that match the required type
     * @param requiredType the target dependency type to match against
     * @return the name of the candidate with the highest priority,
     * or {@code null} if none found
     * @see #getPriority(Object)
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
                    }
                    else if (candidatePriority < highestPriority) {
                        highestPriorityBeanName = candidateBeanName;
                        highestPriority = candidatePriority;
                    }
                }
                else {
                    highestPriorityBeanName = candidateBeanName;
                    highestPriority = candidatePriority;
                }
            }
        }
        return highestPriorityBeanName;
    }

    protected Integer getPriority(Object beanInstance) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).getPriority(beanInstance);
        }
        return null;
    }

    /**
     * Determine whether the given candidate name matches the bean name or the aliases
     * stored in this bean definition.
     */
    protected boolean matchesBeanName(String beanName, String candidateName) {
        return (candidateName != null &&
                (candidateName.equals(beanName)));
    }

    /**
     * Return the dependency comparator for this BeanFactory (may be {@code null}.
     */
    public Comparator<Object> getDependencyComparator() {
        return this.dependencyComparator;
    }

    protected boolean isPrimary(String beanName, Object beanInstance) {
        BeanFactory parentFactory = getParentBeanFactory();
        return (parentFactory instanceof DefaultListableBeanFactory &&
                ((DefaultListableBeanFactory) parentFactory).isPrimary(beanName, beanInstance));
    }
}
