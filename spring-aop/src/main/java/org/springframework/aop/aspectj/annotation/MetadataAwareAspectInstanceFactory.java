package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.AspectInstanceFactory;

public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

    /**
     * Return the AspectJ AspectMetadata for this factory's aspect.
     * @return the aspect metadata
     */
    AspectMetadata getAspectMetadata();

}
