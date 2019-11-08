package org.springframework.aop.aspectj.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} implementation
 * backed by a Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>Note that this may instantiate multiple times if using a prototype,
 * which probably won't give the semantics you expect.
 * Use a {@link LazySingletonAspectInstanceFactoryDecorator}
 * to wrap this to ensure only one new aspect comes back.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 * @since 2.0
 */

public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory {

    private final BeanFactory beanFactory;

    private final String name;

    private final AspectMetadata aspectMetadata;


    /**
     * Create a BeanFactoryAspectInstanceFactory. AspectJ will be called to
     * introspect to create AJType metadata using the type returned for the
     * given bean name from the BeanFactory.
     *
     * @param beanFactory BeanFactory to obtain instance(s) from
     * @param name        name of the bean
     */
    public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
        this(beanFactory, name, beanFactory.getType(name));
    }

    /**
     * Create a BeanFactoryAspectInstanceFactory, providing a type that AspectJ should
     * introspect to create AJType metadata. Use if the BeanFactory may consider the type
     * to be a subclass (as when using CGLIB), and the information should relate to a superclass.
     *
     * @param beanFactory BeanFactory to obtain instance(s) from
     * @param name        the name of the bean
     * @param type        the type that should be introspected by AspectJ
     */
    public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, Class<?> type) {
        this.beanFactory = beanFactory;
        this.name = name;
        this.aspectMetadata = new AspectMetadata(type, name);
    }


    @Override
    public Object getAspectInstance() {
        return this.beanFactory.getBean(this.name);
    }

    @Override
    public ClassLoader getAspectClassLoader() {
        if (this.beanFactory instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader();
        } else {
            return ClassUtils.getDefaultClassLoader();
        }
    }

    @Override
    public AspectMetadata getAspectMetadata() {
        return this.aspectMetadata;
    }

    /**
     * Determine the order for this factory's target aspect, either
     * an instance-specific order expressed through implementing the
     * {@link org.springframework.core.Ordered} interface (only
     * checked for singleton beans), or an order expressed through the
     * {@link org.springframework.core.annotation.Order} annotation
     * at the class level.
     *
     * @see org.springframework.core.Ordered
     * @see org.springframework.core.annotation.Order
     */
    @Override
    public int getOrder() {
        Class<?> type = this.beanFactory.getType(this.name);
        if (type != null) {
            if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
                return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
            }
            return OrderUtils.getOrder(type, Ordered.LOWEST_PRECEDENCE);
        }
        return Ordered.LOWEST_PRECEDENCE;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + ": bean name '" + this.name + "'";
    }

}
