package org.springframework.core.type;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {
    private final boolean nestedAnnotationsAsMap;

    public StandardAnnotationMetadata(Class<?> introspectedClass, boolean nestedAnnotationsAsMap) {
        super(introspectedClass);
        this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
    }

    @Override
    public Set<String> getAnnotationTypes() {
        Set<String> types = new LinkedHashSet<String>();
        Annotation[] anns = getIntrospectedClass().getAnnotations();
        for (Annotation ann : anns) {
            types.add(ann.annotationType().getName());
        }
        return types;
    }

    @Override
    public Set<String> getMetaAnnotationTypes(String annotationName) {
        return AnnotatedElementUtils.getMetaAnnotationTypes(getIntrospectedClass(), annotationName);
    }

    @Override
    public boolean isAnnotated(String annotationName) {
        return AnnotatedElementUtils.isAnnotated(getIntrospectedClass(), annotationName);
    }

    @Override
    public Map<String, Object> getAnnotationAttributes(String annotationName) {
        return AnnotatedElementUtils.getMergedAnnotationAttributes(getIntrospectedClass(),
                annotationName,false, this.nestedAnnotationsAsMap);
    }

    @Override
    public Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        return this.getAnnotationAttributes(annotationName, false);
    }

    @Override
    public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
        return getAllAnnotationAttributes(annotationName, false);
    }

    @Override
    public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        return AnnotatedElementUtils.getAllAnnotationAttributes(getIntrospectedClass(),
                annotationName, classValuesAsString, this.nestedAnnotationsAsMap);
    }
    @Override
    public boolean hasAnnotation(String annotationName) {
        Annotation[] anns = getIntrospectedClass().getAnnotations();
        for (Annotation ann : anns) {
            if (ann.annotationType().getName().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasMetaAnnotation(String annotationName) {
        return AnnotatedElementUtils.hasMetaAnnotationTypes(getIntrospectedClass(), annotationName);
    }

}
