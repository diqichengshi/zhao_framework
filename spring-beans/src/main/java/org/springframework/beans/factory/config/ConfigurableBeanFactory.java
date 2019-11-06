package org.springframework.beans.factory.config;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.config.SingletonBeanRegistry;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
/**
 * Configuration interface to be implemented by most bean factories. Provides
 * facilities to configure a bean factory, in addition to the bean factory
 * client methods in the {@link org.springframework.beans.factory.BeanFactory}
 * interface.
 *
 * <p>This bean factory interface is not meant to be used in normal application
 * code: Stick to {@link org.springframework.beans.factory.BeanFactory} or
 * {@link org.springframework.beans.factory.ListableBeanFactory} for typical
 * needs. This extended interface is just meant to allow for framework-internal
 * plug'n'play and for special access to bean factory configuration methods.
 *
 *作用:实现可配置的bean的环境功能,这个接口继承自HierarchicalBeanFactory所以支持层级关系的工厂,
 * 和SingletonBeanRegistry所以肯定支持单例工厂行为,看主要方法代码(在AbstractBeanFactory类中默认实现)
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see ConfigurableListableBeanFactory
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {
    /**
     * Set the class loader to use for loading bean classes.
     * Default is the thread context class loader.
     * <p>Note that this class loader will only apply to bean definitions
     * that do not carry a resolved bean class yet. This is the case as of
     * Spring 2.0 by default: Bean definitions only carry bean class names,
     * to be resolved once the factory processes the bean definition.
     * @param beanClassLoader the class loader to use,
     * or {@code null} to suggest the default class loader
     */
    void setBeanClassLoader(ClassLoader beanClassLoader);
    /**
     * Return this factory's class loader for loading bean classes.
     */
    ClassLoader getBeanClassLoader();

    /**
     * Specify a temporary ClassLoader to use for type matching purposes.
     * Default is none, simply using the standard bean ClassLoader.
     * <p>A temporary ClassLoader is usually just specified if
     * <i>load-time weaving</i> is involved, to make sure that actual bean
     * classes are loaded as lazily as possible. The temporary loader is
     * then removed once the BeanFactory completes its bootstrap phase.
     * @since 2.5
     */
    void setTempClassLoader(ClassLoader tempClassLoader);
    /**
     * Add a PropertyEditorRegistrar to be applied to all bean creation processes.
     * <p>Such a registrar creates new PropertyEditor instances and registers them
     * on the given registry, fresh for each bean creation attempt. This avoids
     * the need for synchronization on custom editors; hence, it is generally
     * preferable to use this method instead of {@link #registerCustomEditor}.
     * @param registrar the PropertyEditorRegistrar to register
     */
    void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);
    /**
     * Obtain a type converter as used by this BeanFactory. This may be a fresh
     * instance for each call, since TypeConverters are usually <i>not</i> thread-safe.
     * <p>If the default PropertyEditor mechanism is active, the returned
     * TypeConverter will be aware of all custom editors that have been registered.
     * @since 2.5
     */
    TypeConverter getTypeConverter();

    /**
     * Add a new BeanPostProcessor that will get applied to beans created
     * by this factory. To be invoked during factory configuration.
     * <p>Note: Post-processors submitted here will be applied in the order of
     * registration; any ordering semantics expressed through implementing the
     * {@link org.springframework.core.Ordered} interface will be ignored. Note
     * that autodetected post-processors (e.g. as beans in an ApplicationContext)
     * will always be applied after programmatically registered ones.
     * @param beanPostProcessor the post-processor to register
     */
    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);
    /**
     * Determine whether the specified bean is currently in creation.
     * @param beanName the name of the bean
     * @return whether the bean is currently in creation
     * @since 2.5
     */
    boolean isCurrentlyInCreation(String beanName);
}
