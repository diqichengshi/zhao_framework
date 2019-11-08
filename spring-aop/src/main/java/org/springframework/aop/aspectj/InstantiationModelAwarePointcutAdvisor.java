package org.springframework.aop.aspectj;

import org.springframework.aop.PointcutAdvisor;

public interface InstantiationModelAwarePointcutAdvisor extends PointcutAdvisor {

    /**
     * Return whether this advisor is lazily initializing its underlying advice.
     */
    boolean isLazy();

    /**
     * Return whether this advisor has already instantiated its advice.
     */
    boolean isAdviceInstantiated();

}
