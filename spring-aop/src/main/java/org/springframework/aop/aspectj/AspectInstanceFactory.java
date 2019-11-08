package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;

public interface AspectInstanceFactory extends Ordered {

    /**
     * Create an instance of this factory's aspect.
     * @return the aspect instance (never {@code null})
     */
    Object getAspectInstance();

    /**
     * Expose the aspect class loader that this factory uses.
     * @return the aspect class loader (never {@code null})
     */
    ClassLoader getAspectClassLoader();
}
