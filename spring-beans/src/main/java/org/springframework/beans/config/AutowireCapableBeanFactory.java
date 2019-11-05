package org.springframework.beans.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;

import java.util.Set;

public interface AutowireCapableBeanFactory extends BeanFactory {
    /**
     * Constant that indicates no externally defined autowiring. Note that
     * BeanFactoryAware etc and annotation-driven injection will still be applied.
     *
     * @see #createBean
     * @see #autowire
     * @see #autowireBeanProperties
     */
    int AUTOWIRE_NO = 0;

    /**
     * Constant that indicates autowiring bean properties by name
     * (applying to all bean property setters).
     *
     * @see #createBean
     * @see #autowire
     * @see #autowireBeanProperties
     */
    int AUTOWIRE_BY_NAME = 1;

    /**
     * Constant that indicates autowiring bean properties by type
     * (applying to all bean property setters).
     *
     * @see #createBean
     * @see #autowire
     * @see #autowireBeanProperties
     */
    int AUTOWIRE_BY_TYPE = 2;

    /**
     * Constant that indicates autowiring the greediest constructor that
     * can be satisfied (involves resolving the appropriate constructor).
     *
     * @see #createBean
     * @see #autowire
     */
    int AUTOWIRE_CONSTRUCTOR = 3;

    /**
     * Constant that indicates determining an appropriate autowire strategy
     * through introspection of the bean class.
     *
     * @see #createBean
     * @see #autowire
     * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
     * prefer annotation-based autowiring for clearer demarcation of autowiring needs.
     */
    @Deprecated
    int AUTOWIRE_AUTODETECT = 4;

    Object resolveDependency(DependencyDescriptor descriptor, String beanName,
                             Set<String> autowiredBeanNames/*, TypeConverter typeConverter*/) throws BeansException;

}
