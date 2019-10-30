package org.springframework.core.type;

import java.util.Set;

public interface AnnotationMetadata extends ClassMetadata,AnnotatedTypeMetadata{
    /**
     * Get the fully qualified class names of all annotation types that
     * are <em>present</em> on the underlying class.
     * @return the annotation type names
     */
    Set<String> getAnnotationTypes();
    /**
     * Get the fully qualified class names of all meta-annotation types that
     * are <em>present</em> on the given annotation type on the underlying class.
     * @param annotationName the fully qualified class name of the meta-annotation
     * type to look for
     * @return the meta-annotation type names
     */
    Set<String> getMetaAnnotationTypes(String annotationName);

    /**
     * Determine whether an annotation of the given type is <em>present</em> on
     * the underlying class.
     * @param annotationName the fully qualified class name of the annotation
     * type to look for
     * @return {@code true} if a matching annotation is present
     */
    boolean hasAnnotation(String annotationName);

    /**
     * Determine whether the underlying class has an annotation that is itself
     * annotated with the meta-annotation of the given type.
     * @param metaAnnotationName the fully qualified class name of the
     * meta-annotation type to look for
     * @return {@code true} if a matching meta-annotation is present
     */
    boolean hasMetaAnnotation(String metaAnnotationName);

}
