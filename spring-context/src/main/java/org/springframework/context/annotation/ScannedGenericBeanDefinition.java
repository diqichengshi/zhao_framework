package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;

public class ScannedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

    private final AnnotationMetadata metadata;


    /**
     * Create a new ScannedGenericBeanDefinition for the class that the
     * given MetadataReader describes.
     *
     * @param metadataReader the MetadataReader for the scanned target class
     */
    public ScannedGenericBeanDefinition(MetadataReader metadataReader) {
        Assert.notNull(metadataReader, "MetadataReader must not be null");
        this.metadata = metadataReader.getAnnotationMetadata();
        // setBeanClassName(this.metadata.getClassName());
    }


    @Override
    public final AnnotationMetadata getMetadata() {
        return this.metadata;
    }

    /*@Override
    public MethodMetadata getFactoryMethodMetadata() {
        return null;
    }*/

}
