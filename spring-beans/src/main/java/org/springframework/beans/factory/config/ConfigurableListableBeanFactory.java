package org.springframework.beans.factory.config;

import org.springframework.beans.config.AutowireCapableBeanFactory;
import org.springframework.beans.exception.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * 大多数可列出bean工厂要实现的配置接口
 */
/**
 * Configuration interface to be implemented by most listable bean factories.
 * In addition to {@link ConfigurableBeanFactory}, it provides facilities to
 * analyze and modify bean definitions, and to pre-instantiate singletons.
 *
 * <p>This subinterface of {@link org.springframework.beans.factory.BeanFactory}
 * is not meant to be used in normal application code: Stick to
 * {@link org.springframework.beans.factory.BeanFactory} or
 * {@link org.springframework.beans.factory.ListableBeanFactory} for typical
 * use cases. This interface is just meant to allow for framework-internal
 * plug'n'play even when needing access to bean factory configuration methods.
 *
 * 作用：提供可配置的、可访问的功能,接口中的方法在在DefaultListableBeanFactory默认实现默认实现。
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactory()
 */
public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory,ConfigurableBeanFactory {
    /**
     * Ignore the given dependency interface for autowiring.
     * <p>This will typically be used by application contexts to register
     * dependencies that are resolved in other ways, like BeanFactory through
     * BeanFactoryAware or ApplicationContext through ApplicationContextAware.
     * <p>By default, only the BeanFactoryAware interface is ignored.
     * For further types to ignore, invoke this method for each type.
     * @param ifc the dependency interface to ignore
     * @see org.springframework.beans.factory.BeanFactoryAware
     * @see org.springframework.context.ApplicationContextAware
     */
    void ignoreDependencyInterface(Class<?> ifc);
    /**
     * Register a special dependency type with corresponding autowired value.
     * <p>This is intended for factory/context references that are supposed
     * to be autowirable but are not defined as beans in the factory:
     * e.g. a dependency of type ApplicationContext resolved to the
     * ApplicationContext instance that the bean is living in.
     * <p>Note: There are no such default types registered in a plain BeanFactory,
     * not even for the BeanFactory interface itself.
     * @param dependencyType the dependency type to register. This will typically
     * be a base interface such as BeanFactory, with extensions of it resolved
     * as well if declared as an autowiring dependency (e.g. ListableBeanFactory),
     * as long as the given value actually implements the extended interface.
     * @param autowiredValue the corresponding autowired value. This may also be an
     * implementation of the {@link org.springframework.beans.factory.ObjectFactory}
     * interface, which allows for lazy resolution of the actual target value.
     */
    void registerResolvableDependency(Class<?> dependencyType, Object autowiredValue);

    /**
     * Determine whether the specified bean qualifies as an autowire candidate,
     * to be injected into other beans which declare a dependency of matching type.
     * <p>This method checks ancestor factories as well.
     * @param beanName the name of the bean to check
     * @param descriptor the descriptor of the dependency to resolve
     * @return whether the bean should be considered as autowire candidate
     * @throws NoSuchBeanDefinitionException if there is no bean with the given name
     */
    boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
            throws NoSuchBeanDefinitionException;
    /**
     * Return the registered BeanDefinition for the specified bean, allowing access
     * to its property values and constructor argument value (which can be
     * modified during bean factory post-processing).
     * <p>A returned BeanDefinition object should not be a copy but the original
     * definition object as registered in the factory. This means that it should
     * be castable to a more specific implementation type, if necessary.
     * <p><b>NOTE:</b> This method does <i>not</i> consider ancestor factories.
     * It is only meant for accessing local bean definitions of this factory.
     *
     * @param beanName the name of the bean
     * @return the registered BeanDefinition
     * @throws NoSuchBeanDefinitionException if there is no bean with the given name
     *                                       defined in this factory
     */
    BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;
    /**
     * Clear the merged bean definition cache, removing entries for beans
     * which are not considered eligible for full metadata caching yet.
     * <p>Typically triggered after changes to the original bean definitions,
     * e.g. after applying a {@link BeanFactoryPostProcessor}. Note that metadata
     * for beans which have already been created at this point will be kept around.
     * @since 4.2
     * @see #getBeanDefinition
     * @see #getMergedBeanDefinition
     */
    void clearMetadataCache();
}
