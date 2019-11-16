package org.springframework.beans.factory;


import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

public class BeanFactoryUtils {
    public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";


    public static boolean isFactoryDereference(String name) {
        return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
    }

    public static String transformedBeanName(String name) {
        Assert.notNull(name, "'name' must not be null");
        String beanName = name;
        while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
            beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
        }
        return beanName;
    }

    /**
     * Return whether the given name is a bean name which has been generated
     * by the default naming strategy (containing a "#..." part).
     * @param name the name of the bean
     * @return whether the given name is a generated bean name
     * @see #GENERATED_BEAN_NAME_SEPARATOR
     * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#generateBeanName
     * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
     */
    public static boolean isGeneratedBeanName(String name) {
        return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
    }

    /**
     * Get all bean names for the given type, including those defined in ancestor
     * factories. Will return unique names in case of overridden bean definitions.
     * <p>Does consider objects created by FactoryBeans if the "allowEagerInit"
     * flag is set, which means that FactoryBeans will get initialized. If the
     * object created by the FactoryBean doesn't match, the raw FactoryBean itself
     * will be matched against the type. If "allowEagerInit" is not set,
     * only raw FactoryBeans will be checked (which doesn't require initialization
     * of each FactoryBean).
     * @param lbf the bean factory
     * @param includeNonSingletons whether to include prototype or scoped beans too
     * or just singletons (also applies to FactoryBeans)
     * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
     * <i>objects created by FactoryBeans</i> (or by factory methods with a
     * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
     * eagerly initialized to determine their type: So be aware that passing in "true"
     * for this flag will initialize FactoryBeans and "factory-bean" references.
     * @param type the type that beans must match
     * @return the array of matching bean names, or an empty array if none
     */
    public static String[] beanNamesForTypeIncludingAncestors(
            ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

        Assert.notNull(lbf, "ListableBeanFactory must not be null");
        String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        if (lbf instanceof HierarchicalBeanFactory) {
            HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
            if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
                String[] parentResult = beanNamesForTypeIncludingAncestors(
                        (ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
                List<String> resultList = new ArrayList<String>();
                resultList.addAll(Arrays.asList(result));
                for (String beanName : parentResult) {
                    if (!resultList.contains(beanName) && !hbf.containsLocalBean(beanName)) {
                        resultList.add(beanName);
                    }
                }
                result = StringUtils.toStringArray(resultList);
            }
        }
        return result;
    }


    /**
     * Return all beans of the given type or subtypes, also picking up beans defined in
     * ancestor bean factories if the current bean factory is a HierarchicalBeanFactory.
     * The returned Map will only contain beans of this type.
     * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
     * will get initialized. If the object created by the FactoryBean doesn't match,
     * the raw FactoryBean itself will be matched against the type.
     * <p><b>Note: Beans of the same name will take precedence at the 'lowest' factory level,
     * i.e. such beans will be returned from the lowest factory that they are being found in,
     * hiding corresponding beans in ancestor factories.</b> This feature allows for
     * 'replacing' beans by explicitly choosing the same bean name in a child factory;
     * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
     * @param lbf the bean factory
     * @param type type of bean to match
     * @return the Map of matching bean instances, or an empty Map if none
     * @throws BeansException if a bean could not be created
     */
    public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
            throws BeansException {

        Assert.notNull(lbf, "ListableBeanFactory must not be null");
        Map<String, T> result = new LinkedHashMap<String, T>(4);
        result.putAll(lbf.getBeansOfType(type));
        if (lbf instanceof HierarchicalBeanFactory) {
            HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
            if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
                Map<String, T> parentResult = beansOfTypeIncludingAncestors(
                        (ListableBeanFactory) hbf.getParentBeanFactory(), type);
                for (Map.Entry<String, T> entry : parentResult.entrySet()) {
                    String beanName = entry.getKey();
                    if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
                        result.put(beanName, entry.getValue());
                    }
                }
            }
        }
        return result;
    }

}
