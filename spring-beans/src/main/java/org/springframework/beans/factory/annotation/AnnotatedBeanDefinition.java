package org.springframework.beans.factory.annotation;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.core.type.AnnotationMetadata;

public interface AnnotatedBeanDefinition extends BeanDefinition {
    /**
     * Obtain the annotation metadata (as well as basic class metadata)
     * for this bean definition's bean class.
     * @return the annotation metadata object (never {@code null})
     */
    AnnotationMetadata getMetadata();
}
