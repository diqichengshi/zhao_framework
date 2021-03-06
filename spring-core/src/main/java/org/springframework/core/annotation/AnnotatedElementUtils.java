package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class AnnotatedElementUtils {

    private static final Boolean CONTINUE = null;

    private static final Processor<Boolean> alwaysTrueAnnotationProcessor = new AlwaysTrueBooleanAnnotationProcessor();

    /**
     * Get the fully qualified class names of all meta-annotation
     * types <em>present</em> on the annotation (of the specified
     * {@code annotationType}) on the supplied {@link AnnotatedElement}.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationType the annotation type on which to find
     *                       meta-annotations; never {@code null}
     * @return the names of all meta-annotations present on the annotation,
     * or {@code null} if not found
     * @see #getMetaAnnotationTypes(AnnotatedElement, String)
     * @see #hasMetaAnnotationTypes
     * @since 4.2
     */
    public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        Assert.notNull(annotationType, "annotationType must not be null");
        return getMetaAnnotationTypes(element, annotationType.getName());
    }

    /**
     * Get the fully qualified class names of all meta-annotation
     * types <em>present</em> on the annotation (of the specified
     * {@code annotationName}) on the supplied {@link AnnotatedElement}.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type on which to find meta-annotations; never {@code null} or empty
     * @return the names of all meta-annotations present on the annotation,
     * or {@code null} if not found
     * @see #getMetaAnnotationTypes(AnnotatedElement, Class)
     * @see #hasMetaAnnotationTypes
     */
    public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
        Assert.notNull(element, "AnnotatedElement must not be null");
        Assert.hasLength(annotationName, "annotationName must not be null or empty");

        final Set<String> types = new LinkedHashSet<String>();

        try {
            Annotation annotation = AnnotationUtils.getAnnotation(element, annotationName);
            if (annotation != null) {
                searchWithGetSemantics(annotation.annotationType(), annotationName, new SimpleAnnotationProcessor<Object>() {

                    @Override
                    public Object process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
                        types.add(annotation.annotationType().getName());
                        return CONTINUE;
                    }
                }, new HashSet<AnnotatedElement>(), 1);
            }
        } catch (Throwable ex) {
            AnnotationUtils.rethrowAnnotationConfigurationException(ex);
            throw new IllegalStateException("Failed to introspect annotations on " + element, ex);
        }

        return (!types.isEmpty() ? types : null);
    }

    /**
     * Determine if the supplied {@link AnnotatedElement} is annotated with
     * a <em>composed annotation</em> that is meta-annotated with an
     * annotation of the specified {@code annotationName}.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the meta-annotation
     *                       type to find; never {@code null} or empty
     * @return {@code true} if a matching meta-annotation is present
     * @see #getMetaAnnotationTypes
     */
    public static boolean hasMetaAnnotationTypes(AnnotatedElement element, final String annotationName) {
        Assert.notNull(element, "AnnotatedElement must not be null");
        Assert.hasLength(annotationName, "annotationName must not be null or empty");

        return Boolean.TRUE.equals(searchWithGetSemantics(element, annotationName, new SimpleAnnotationProcessor<Boolean>() {
            @Override
            public Boolean process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
                boolean found = annotation.annotationType().getName().equals(annotationName);
                return (found && metaDepth > 0 ? Boolean.TRUE : CONTINUE);
            }
        }));
    }

    /**
     * Determine if an annotation of the specified {@code annotationName}
     * is <em>present</em> on the supplied {@link AnnotatedElement} or
     * within the annotation hierarchy <em>above</em> the specified element.
     * <p>If this method returns {@code true}, then {@link #getMergedAnnotationAttributes}
     * will return a non-null value.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @return {@code true} if a matching annotation is present
     */
    public static boolean isAnnotated(AnnotatedElement element, final String annotationName) {
        Assert.notNull(element, "AnnotatedElement must not be null");
        Assert.hasLength(annotationName, "annotationName must not be null or empty");

        return Boolean.TRUE.equals(searchWithGetSemantics(element, annotationName, new SimpleAnnotationProcessor<Boolean>() {
            @Override
            public Boolean process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
                boolean found = annotation.annotationType().getName().equals(annotationName);
                return (found ? Boolean.TRUE : CONTINUE);
            }
        }));
    }

    /**
     * Get the first annotation of the specified {@code annotationType} within
     * the annotation hierarchy <em>above</em> the supplied {@code element},
     * merge that annotation's attributes with <em>matching</em> attributes from
     * annotations in lower levels of the annotation hierarchy, and synthesize
     * the result back into an annotation of the specified {@code annotationType}.
     * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
     * within a single annotation and within the annotation hierarchy.
     * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, Class)}
     * and {@link AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationType the annotation type to find; never {@code null}
     * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
     * @see #getMergedAnnotationAttributes(AnnotatedElement, Class)
     * @see #findMergedAnnotation(AnnotatedElement, Class)
     * @see AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)
     * @since 4.2
     */
    public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        AnnotationAttributes attributes = getMergedAnnotationAttributes(element, annotationType);
        return ((attributes != null) ? AnnotationUtils.synthesizeAnnotation(attributes, annotationType, element) : null);
    }

    /**
     * Get the first annotation of the specified {@code annotationType} within
     * the annotation hierarchy <em>above</em> the supplied {@code element} and
     * merge that annotation's attributes with <em>matching</em> attributes from
     * annotations in lower levels of the annotation hierarchy.
     * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
     * within a single annotation and within the annotation hierarchy.
     * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationType the annotation type to find; never {@code null}
     * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
     * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @see #getMergedAnnotation(AnnotatedElement, Class)
     * @see #findMergedAnnotation(AnnotatedElement, Class)
     * @since 4.2
     */
    public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        Assert.notNull(annotationType, "annotationType must not be null");
        return getMergedAnnotationAttributes(element, annotationType.getName());
    }

    /**
     * @deprecated As of Spring Framework 4.2, use {@link #getMergedAnnotationAttributes(AnnotatedElement, String)} instead.
     */
    @Deprecated
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationName) {
        return getMergedAnnotationAttributes(element, annotationName);
    }

    /**
     * Get the first annotation of the specified {@code annotationName} within
     * the annotation hierarchy <em>above</em> the supplied {@code element} and
     * merge that annotation's attributes with <em>matching</em> attributes from
     * annotations in lower levels of the annotation hierarchy.
     * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
     * within a single annotation and within the annotation hierarchy.
     * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)},
     * supplying {@code false} for {@code classValuesAsString} and {@code nestedAnnotationsAsMap}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @return the merged {@code AnnotationAttributes}, or {@code null} if
     * not found
     * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @see #findMergedAnnotation(AnnotatedElement, Class)
     * @see #getAllAnnotationAttributes(AnnotatedElement, String)
     * @since 4.2
     */
    public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element, String annotationName) {
        return getMergedAnnotationAttributes(element, annotationName, false, false);
    }

    /**
     * @deprecated As of Spring Framework 4.2, use {@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)} instead.
     */
    @Deprecated
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationName,
                                                               boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

        return getMergedAnnotationAttributes(element, annotationName, classValuesAsString, nestedAnnotationsAsMap);
    }

    /**
     * Get the first annotation of the specified {@code annotationName} within
     * the annotation hierarchy <em>above</em> the supplied {@code element} and
     * merge that annotation's attributes with <em>matching</em> attributes from
     * annotations in lower levels of the annotation hierarchy.
     * <p>Attributes from lower levels in the annotation hierarchy override
     * attributes of the same name from higher levels, and
     * {@link AliasFor @AliasFor} semantics are fully supported, both
     * within a single annotation and within the annotation hierarchy.
     * <p>In contrast to {@link #getAllAnnotationAttributes}, the search
     * algorithm used by this method will stop searching the annotation
     * hierarchy once the first annotation of the specified
     * {@code annotationName} has been found. As a consequence, additional
     * annotations of the specified {@code annotationName} will be ignored.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element                the annotated element; never {@code null}
     * @param annotationName         the fully qualified class name of the annotation
     *                               type to find; never {@code null} or empty
     * @param classValuesAsString    whether to convert Class references into
     *                               Strings or to preserve them as Class references
     * @param nestedAnnotationsAsMap whether to convert nested Annotation
     *                               instances into {@code AnnotationAttributes} maps or to preserve them
     *                               as Annotation instances
     * @return the merged {@code AnnotationAttributes}, or {@code null} if
     * not found
     * @see #findMergedAnnotation(AnnotatedElement, Class)
     * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @since 4.2
     */
    public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element, String annotationName,
                                                                     boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

        AnnotationAttributes attributes = searchWithGetSemantics(element, annotationName,
                new MergedAnnotationAttributesProcessor(annotationName, classValuesAsString, nestedAnnotationsAsMap));
        AnnotationUtils.postProcessAnnotationAttributes(element, attributes, classValuesAsString, nestedAnnotationsAsMap);
        return attributes;
    }

    /**
     * Find the first annotation of the specified {@code annotationType} within
     * the annotation hierarchy <em>above</em> the supplied {@code element},
     * merge that annotation's attributes with <em>matching</em> attributes from
     * annotations in lower levels of the annotation hierarchy, and synthesize
     * the result back into an annotation of the specified {@code annotationType}.
     * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
     * within a single annotation and within the annotation hierarchy.
     * <p>This method delegates to {@link #findMergedAnnotation(AnnotatedElement, String)}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationType the annotation type to find; never {@code null}
     * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
     * @see #findMergedAnnotation(AnnotatedElement, String)
     * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @see #getMergedAnnotationAttributes(AnnotatedElement, Class)
     * @since 4.2
     */
    public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        Assert.notNull(annotationType, "annotationType must not be null");
        return findMergedAnnotation(element, annotationType.getName());
    }

    /**
     * Find the first annotation of the specified {@code annotationName} within
     * the annotation hierarchy <em>above</em> the supplied {@code element},
     * merge that annotation's attributes with <em>matching</em> attributes from
     * annotations in lower levels of the annotation hierarchy, and synthesize
     * the result back into an annotation of the specified {@code annotationName}.
     * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
     * within a single annotation and within the annotation hierarchy.
     * <p>This method delegates to {@link #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)}
     * (supplying {@code false} for {@code classValuesAsString} and {@code nestedAnnotationsAsMap})
     * and {@link AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
     * @see #findMergedAnnotation(AnnotatedElement, Class)
     * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @see AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)
     * @since 4.2
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, String annotationName) {
        AnnotationAttributes attributes = findMergedAnnotationAttributes(element, annotationName, false, false);
        return (attributes != null ?
                AnnotationUtils.synthesizeAnnotation(attributes, (Class<A>) attributes.annotationType(), element) : null);
    }

    /**
     * Find the first annotation of the specified {@code annotationName} within
     * the annotation hierarchy <em>above</em> the supplied {@code element} and
     * merge that annotation's attributes with <em>matching</em> attributes from
     * annotations in lower levels of the annotation hierarchy.
     * <p>Attributes from lower levels in the annotation hierarchy override
     * attributes of the same name from higher levels, and
     * {@link AliasFor @AliasFor} semantics are fully supported, both
     * within a single annotation and within the annotation hierarchy.
     * <p>In contrast to {@link #getAllAnnotationAttributes}, the search
     * algorithm used by this method will stop searching the annotation
     * hierarchy once the first annotation of the specified
     * {@code annotationName} has been found. As a consequence, additional
     * annotations of the specified {@code annotationName} will be ignored.
     * <p>This method follows <em>find semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element                the annotated element; never {@code null}
     * @param annotationName         the fully qualified class name of the annotation
     *                               type to find; never {@code null} or empty
     * @param classValuesAsString    whether to convert Class references into
     *                               Strings or to preserve them as Class references
     * @param nestedAnnotationsAsMap whether to convert nested Annotation
     *                               instances into {@code AnnotationAttributes} maps or to preserve them
     *                               as Annotation instances
     * @return the merged {@code AnnotationAttributes}, or {@code null} if
     * not found
     * @see #findMergedAnnotation(AnnotatedElement, Class)
     * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     * @since 4.2
     */
    public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element, String annotationName,
                                                                      boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

        AnnotationAttributes attributes = searchWithFindSemantics(element, annotationName,
                new MergedAnnotationAttributesProcessor(annotationName, classValuesAsString, nestedAnnotationsAsMap));
        AnnotationUtils.postProcessAnnotationAttributes(element, attributes, classValuesAsString, nestedAnnotationsAsMap);
        return attributes;
    }

    /**
     * Get the annotation attributes of <strong>all</strong> annotations of
     * the specified {@code annotationName} in the annotation hierarchy above
     * the supplied {@link AnnotatedElement} and store the results in a
     * {@link MultiValueMap}.
     * <p>Note: in contrast to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)},
     * this method does <em>not</em> support attribute overrides.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @return a {@link MultiValueMap} keyed by attribute name, containing
     * the annotation attributes from all annotations found, or {@code null}
     * if not found
     * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
     */
    public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element, String annotationName) {
        return getAllAnnotationAttributes(element, annotationName, false, false);
    }

    /**
     * Get the annotation attributes of <strong>all</strong> annotations of
     * the specified {@code annotationName} in the annotation hierarchy above
     * the supplied {@link AnnotatedElement} and store the results in a
     * {@link MultiValueMap}.
     * <p>Note: in contrast to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)},
     * this method does <em>not</em> support attribute overrides.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level Javadoc}.
     *
     * @param element                the annotated element; never {@code null}
     * @param annotationName         the fully qualified class name of the annotation
     *                               type to find; never {@code null} or empty
     * @param classValuesAsString    whether to convert Class references into
     *                               Strings or to preserve them as Class references
     * @param nestedAnnotationsAsMap whether to convert nested Annotation
     *                               instances into {@code AnnotationAttributes} maps or to preserve them
     *                               as Annotation instances
     * @return a {@link MultiValueMap} keyed by attribute name, containing
     * the annotation attributes from all annotations found, or {@code null}
     * if not found
     */
    public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
                                                                           final String annotationName, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

        final MultiValueMap<String, Object> attributesMap = new LinkedMultiValueMap<String, Object>();

        searchWithGetSemantics(element, annotationName, new SimpleAnnotationProcessor<Void>() {
            @Override
            public Void process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
                boolean found = annotation.annotationType().getName().equals(annotationName);
                if (found) {
                    AnnotationAttributes annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotation,
                            classValuesAsString, nestedAnnotationsAsMap);
                    for (Map.Entry<String, Object> entry : annotationAttributes.entrySet()) {
                        attributesMap.add(entry.getKey(), entry.getValue());
                    }
                }
                // Continue searching...
                return null;
            }
        });

        return (!attributesMap.isEmpty() ? attributesMap : null);
    }

    /**
     * Search for annotations of the specified {@code annotationName} on
     * the specified {@code element}, following <em>get semantics</em>.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @param processor      the processor to delegate to
     * @return the result of the processor, potentially {@code null}
     */
    private static <T> T searchWithGetSemantics(AnnotatedElement element, String annotationName, Processor<T> processor) {
        try {
            return searchWithGetSemantics(element, annotationName, processor, new HashSet<AnnotatedElement>(), 0);
        } catch (Throwable ex) {
            AnnotationUtils.rethrowAnnotationConfigurationException(ex);
            throw new IllegalStateException("Failed to introspect annotations on " + element, ex);
        }
    }

    /**
     * Perform the search algorithm for the {@link #searchWithGetSemantics}
     * method, avoiding endless recursion by tracking which annotated elements
     * have already been <em>visited</em>.
     * <p>The {@code metaDepth} parameter is explained in the
     * {@link Processor#process process()} method of the {@link Processor} API.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @param processor      the processor to delegate to
     * @param visited        the set of annotated elements that have already been visited
     * @param metaDepth      the meta-depth of the annotation
     * @return the result of the processor, potentially {@code null}
     */
    private static <T> T searchWithGetSemantics(AnnotatedElement element, String annotationName,
                                                Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

        Assert.notNull(element, "AnnotatedElement must not be null");
        Assert.hasLength(annotationName, "annotationName must not be null or empty");

        if (visited.add(element)) {
            try {
                // Start searching within locally declared annotations
                List<Annotation> declaredAnnotations = Arrays.asList(element.getDeclaredAnnotations());
                T result = searchWithGetSemanticsInAnnotations(element, declaredAnnotations, annotationName, processor,
                        visited, metaDepth);
                if (result != null) {
                    return result;
                }

                List<Annotation> inheritedAnnotations = new ArrayList<Annotation>();
                for (Annotation annotation : element.getAnnotations()) {
                    if (!declaredAnnotations.contains(annotation)) {
                        inheritedAnnotations.add(annotation);
                    }
                }

                // Continue searching within inherited annotations
                result = searchWithGetSemanticsInAnnotations(
                        element, inheritedAnnotations, annotationName, processor, visited, metaDepth);
                if (result != null) {
                    return result;
                }
            } catch (Exception ex) {
                AnnotationUtils.handleIntrospectionFailure(element, ex);
            }
        }

        return null;
    }

    /**
     * This method is invoked by
     * {@link #searchWithGetSemantics(AnnotatedElement, String, Processor, Set, int)}
     * to perform the actual search within the supplied list of annotations.
     * <p>This method should be invoked first with locally declared annotations
     * and then subsequently with inherited annotations, thereby allowing
     * local annotations to take precedence over inherited annotations.
     * <p>The {@code metaDepth} parameter is explained in the
     * {@link Processor#process process()} method of the {@link Processor} API.
     *
     * @param annotatedElement the element that is annotated with the supplied
     *                         annotations, used for contextual logging; may be {@code null} if unknown
     * @param annotations      the annotations to search in; never {@code null}
     * @param annotationName   the fully qualified class name of the annotation
     *                         type to find; never {@code null} or empty
     * @param processor        the processor to delegate to
     * @param visited          the set of annotated elements that have already been visited
     * @param metaDepth        the meta-depth of the annotation
     * @return the result of the processor, potentially {@code null}
     * @since 4.2
     */
    private static <T> T searchWithGetSemanticsInAnnotations(AnnotatedElement annotatedElement, List<Annotation> annotations,
                                                             String annotationName, Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

        // Search in annotations
        for (Annotation annotation : annotations) {
            if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation) &&
                    (annotation.annotationType().getName().equals(annotationName) || metaDepth > 0)) {
                T result = processor.process(annotatedElement, annotation, metaDepth);
                if (result != null) {
                    return result;
                }
            }
        }

        // Recursively search in meta-annotations
        for (Annotation annotation : annotations) {
            if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
                T result = searchWithGetSemantics(
                        annotation.annotationType(), annotationName, processor, visited, metaDepth + 1);
                if (result != null) {
                    processor.postProcess(annotatedElement, annotation, result);
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Search for annotations of the specified {@code annotationName} on
     * the specified {@code element}, following <em>find semantics</em>.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @param processor      the processor to delegate to
     * @return the result of the processor, potentially {@code null}
     * @since 4.2
     */
    private static <T> T searchWithFindSemantics(AnnotatedElement element, String annotationName, Processor<T> processor) {
        try {
            return searchWithFindSemantics(element, annotationName, processor, new HashSet<AnnotatedElement>(), 0);
        } catch (Throwable ex) {
            AnnotationUtils.rethrowAnnotationConfigurationException(ex);
            throw new IllegalStateException("Failed to introspect annotations on " + element, ex);
        }
    }

    /**
     * Perform the search algorithm for the {@link #searchWithFindSemantics}
     * method, avoiding endless recursion by tracking which annotated elements
     * have already been <em>visited</em>.
     * <p>The {@code metaDepth} parameter is explained in the
     * {@link Processor#process process()} method of the {@link Processor} API.
     *
     * @param element        the annotated element; never {@code null}
     * @param annotationName the fully qualified class name of the annotation
     *                       type to find; never {@code null} or empty
     * @param processor      the processor to delegate to
     * @param visited        the set of annotated elements that have already been visited
     * @param metaDepth      the meta-depth of the annotation
     * @return the result of the processor, potentially {@code null}
     * @since 4.2
     */
    private static <T> T searchWithFindSemantics(AnnotatedElement element, String annotationName,
                                                 Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

        Assert.notNull(element, "AnnotatedElement must not be null");
        Assert.hasLength(annotationName, "annotationName must not be null or empty");

        if (visited.add(element)) {
            try {
                // Locally declared annotations (ignoring @Inherited)
                Annotation[] annotations = element.getDeclaredAnnotations();

                // Search in local annotations
                for (Annotation annotation : annotations) {
                    if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)
                            && (annotation.annotationType().getName().equals(annotationName) || metaDepth > 0)) {
                        T result = processor.process(element, annotation, metaDepth);
                        if (result != null) {
                            return result;
                        }
                    }
                }

                // Search in meta annotations on local annotations
                for (Annotation annotation : annotations) {
                    if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
                        T result = searchWithFindSemantics(annotation.annotationType(), annotationName, processor,
                                visited, metaDepth + 1);
                        if (result != null) {
                            processor.postProcess(annotation.annotationType(), annotation, result);
                            return result;
                        }
                    }
                }

                if (element instanceof Method) {
                    Method method = (Method) element;

                    // Search on possibly bridged method
                    Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
                    T result = searchWithFindSemantics(resolvedMethod, annotationName, processor, visited, metaDepth);
                    if (result != null) {
                        return result;
                    }

                    // Search on methods in interfaces declared locally
                    Class<?>[] ifcs = method.getDeclaringClass().getInterfaces();
                    result = searchOnInterfaces(method, annotationName, processor, visited, metaDepth, ifcs);
                    if (result != null) {
                        return result;
                    }

                    // Search on methods in class hierarchy and interface hierarchy
                    Class<?> clazz = method.getDeclaringClass();
                    while (true) {
                        clazz = clazz.getSuperclass();
                        if (clazz == null || Object.class == clazz) {
                            break;
                        }

                        try {
                            Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                            Method resolvedEquivalentMethod = BridgeMethodResolver.findBridgedMethod(equivalentMethod);
                            result = searchWithFindSemantics(
                                    resolvedEquivalentMethod, annotationName, processor, visited, metaDepth);
                            if (result != null) {
                                return result;
                            }
                        } catch (NoSuchMethodException ex) {
                            // No equivalent method found
                        }

                        // Search on interfaces declared on superclass
                        result = searchOnInterfaces(method, annotationName, processor, visited, metaDepth,
                                clazz.getInterfaces());
                        if (result != null) {
                            return result;
                        }
                    }
                }

                if (element instanceof Class) {
                    Class<?> clazz = (Class<?>) element;

                    // Search on interfaces
                    for (Class<?> ifc : clazz.getInterfaces()) {
                        T result = searchWithFindSemantics(ifc, annotationName, processor, visited, metaDepth);
                        if (result != null) {
                            return result;
                        }
                    }

                    // Search on superclass
                    Class<?> superclass = clazz.getSuperclass();
                    if (superclass != null && Object.class != superclass) {
                        T result = searchWithFindSemantics(superclass, annotationName, processor, visited, metaDepth);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            } catch (Exception ex) {
                AnnotationUtils.handleIntrospectionFailure(element, ex);
            }
        }
        return null;
    }

    private static <T> T searchOnInterfaces(Method method, String annotationName, Processor<T> processor,
                                            Set<AnnotatedElement> visited, int metaDepth, Class<?>[] ifcs) {

        for (Class<?> iface : ifcs) {
            if (AnnotationUtils.isInterfaceWithAnnotatedMethods(iface)) {
                try {
                    Method equivalentMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                    T result = searchWithFindSemantics(equivalentMethod, annotationName, processor, visited, metaDepth);
                    if (result != null) {
                        return result;
                    }
                } catch (NoSuchMethodException ex) {
                    // Skip this interface - it doesn't have the method...
                }
            }
        }

        return null;
    }


    /**
     * Callback interface that is used to process annotations during a search.
     * <p>Depending on the use case, a processor may choose to
     * {@linkplain #process} a single target annotation, multiple target
     * annotations, or all annotations discovered by the currently executing
     * search. The term "target" in this context refers to a matching
     * annotation (i.e., a specific annotation type that was found during
     * the search). Returning a non-null value from the {@link #process}
     * method instructs the search algorithm to stop searching further;
     * whereas, returning {@code null} from the {@link #process} method
     * instructs the search algorithm to continue searching for additional
     * annotations.
     * <p>Processors can optionally {@linkplain #postProcess post-process}
     * the result of the {@link #process} method as the search algorithm
     * goes back down the annotation hierarchy from an invocation of
     * {@link #process} that returned a non-null value down to the
     * {@link AnnotatedElement} that was supplied as the starting point to
     * the search algorithm.
     *
     * @param <T> the type of result returned by the processor
     */
    private interface Processor<T> {

        /**
         * Process the supplied annotation.
         * <p>Depending on the use case, the supplied annotation may be an
         * actual target annotation that has been found by the search
         * algorithm, or it may be some other annotation within the
         * annotation hierarchy. In the latter case, the {@code metaDepth}
         * should have a value greater than {@code 0}. In any case, it is
         * up to concrete implementations of this method to decide what to
         * do with the supplied annotation.
         * <p>The {@code metaDepth} parameter represents the depth of the
         * annotation relative to the first annotated element in the
         * annotation hierarchy. For example, an annotation that is
         * <em>present</em> on a non-annotation element will have a depth
         * of 0; a meta-annotation will have a depth of 1; and a
         * meta-meta-annotation will have a depth of 2; etc.
         *
         * @param annotatedElement the element that is annotated with the
         *                         supplied annotation, used for contextual logging; may be
         *                         {@code null} if unknown
         * @param annotation       the annotation to process
         * @param metaDepth        the meta-depth of the annotation
         * @return the result of the processing, or {@code null} to continue
         * searching for additional annotations
         */
        T process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth);

        /**
         * Post-process the result returned by the {@link #process} method.
         * <p>The {@code annotation} supplied to this method is an annotation
         * that is present in the annotation hierarchy, between the initial
         * {@link AnnotatedElement} and an invocation of {@link #process}
         * that returned a non-null value.
         *
         * @param annotatedElement the element that is annotated with the
         *                         supplied annotation, used for contextual logging; may be
         *                         {@code null} if unknown
         * @param annotation       the annotation to post-process
         * @param result           the result to post-process
         */
        void postProcess(AnnotatedElement annotatedElement, Annotation annotation, T result);
    }


    /**
     * Determine if an annotation of the specified {@code annotationType}
     * is <em>available</em> on the supplied {@link AnnotatedElement} or
     * within the annotation hierarchy <em>above</em> the specified element.
     * <p>If this method returns {@code true}, then {@link #findMergedAnnotationAttributes}
     * will return a non-null value.
     * <p>This method follows <em>find semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level javadoc}.
     * @param element the annotated element
     * @param annotationType the annotation type to find
     * @return {@code true} if a matching annotation is present
     * @since 4.3
     * @see #isAnnotated(AnnotatedElement, Class)
     */
    public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        Assert.notNull(element, "AnnotatedElement must not be null");
        Assert.notNull(annotationType, "'annotationType' must not be null");

        // Shortcut: directly present on the element, with no processing needed?
        if (element.isAnnotationPresent(annotationType)) {
            return true;
        }
        return Boolean.TRUE.equals(searchWithFindSemantics(element, annotationType.getName(), alwaysTrueAnnotationProcessor));
    }

    /**
     * {@link Processor} that {@linkplain #process processes} annotations
     * but does not {@linkplain #postProcess post-process} results.
     *
     * @since 4.2
     */
    private abstract static class SimpleAnnotationProcessor<T> implements Processor<T> {

        @Override
        public final void postProcess(AnnotatedElement annotatedElement, Annotation annotation, T result) {
            // no-op
        }
    }

    /**
     * {@link SimpleAnnotationProcessor} that always returns {@link Boolean#TRUE} when
     * asked to {@linkplain #process(AnnotatedElement, Annotation, int) process} an
     * annotation.
     * @since 4.3
     */
    static class AlwaysTrueBooleanAnnotationProcessor extends SimpleAnnotationProcessor<Boolean> {

        @Override
        public final Boolean process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
            return Boolean.TRUE;
        }
    }


    /**
     * {@link Processor} that gets the {@code AnnotationAttributes} for the
     * target annotation during the {@link #process} phase and then merges
     * annotation attributes from lower levels in the annotation hierarchy
     * during the {@link #postProcess} phase.
     *
     * @see AnnotationUtils#getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean, boolean)
     * @see AnnotationUtils#postProcessAnnotationAttributes
     * @since 4.2
     */
    private static class MergedAnnotationAttributesProcessor implements Processor<AnnotationAttributes> {

        private final String annotationName;

        private final boolean classValuesAsString;

        private final boolean nestedAnnotationsAsMap;

        MergedAnnotationAttributesProcessor(String annotationName, boolean classValuesAsString,
                                            boolean nestedAnnotationsAsMap) {

            this.annotationName = annotationName;
            this.classValuesAsString = classValuesAsString;
            this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
        }

        @Override
        public AnnotationAttributes process(AnnotatedElement annotatedElement, Annotation annotation, int metaDepth) {
            boolean found = annotation.annotationType().getName().equals(this.annotationName);
            return (found ? AnnotationUtils.getAnnotationAttributes(annotatedElement, annotation,
                    this.classValuesAsString, this.nestedAnnotationsAsMap, true) : null);
        }

        @Override
        public void postProcess(AnnotatedElement element, Annotation annotation, AnnotationAttributes attributes) {
            annotation = AnnotationUtils.synthesizeAnnotation(annotation, element);
            Class<? extends Annotation> targetAnnotationType = attributes.annotationType();

            for (Method attributeMethod : AnnotationUtils.getAttributeMethods(annotation.annotationType())) {
                String attributeName = attributeMethod.getName();
                String attributeOverrideName = AnnotationUtils.getAttributeOverrideName(attributeMethod, targetAnnotationType);

                // Explicit annotation attribute override declared via @AliasFor
                if (attributeOverrideName != null) {
                    if (attributes.containsKey(attributeOverrideName)) {
                        overrideAttribute(element, annotation, attributes, attributeName, attributeOverrideName);
                    }
                }
                // Implicit annotation attribute override based on convention
                else if (!AnnotationUtils.VALUE.equals(attributeName) && attributes.containsKey(attributeName)) {
                    overrideAttribute(element, annotation, attributes, attributeName, attributeName);
                }
            }
        }

        private void overrideAttribute(AnnotatedElement element, Annotation annotation,
                                       AnnotationAttributes attributes, String sourceAttributeName, String targetAttributeName) {

            Object value = AnnotationUtils.getValue(annotation, sourceAttributeName);
            Object adaptedValue = AnnotationUtils.adaptValue(
                    element, value, this.classValuesAsString, this.nestedAnnotationsAsMap);
            attributes.put(targetAttributeName, adaptedValue);
        }
    }

}
