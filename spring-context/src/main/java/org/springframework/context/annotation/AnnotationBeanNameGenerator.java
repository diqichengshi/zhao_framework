package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.util.Map;
import java.util.Set;

public class AnnotationBeanNameGenerator implements BeanNameGenerator {

    private static final String COMPONENT_ANNOTATION_CLASSNAME = "org.springframework.stereotype.Component";

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        // 判断是AnnotatedBeanDefinition的实现,就从annotation获得
        if (definition instanceof AnnotatedBeanDefinition) {
            String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
            // 是文本就返回这个beanName,但是也有可能annotation的value是null,就后从buildDefaultBeanName获得
            if (StringUtils.hasText(beanName)) {
                // Explicit bean name found.
                return beanName;
            }
        }
        // Fallback: generate a unique default bean name.
        return buildDefaultBeanName(definition, registry);
    }

    /**
     * Derive a bean name from one of the annotations on the class.
     *
     * @param annotatedDef the annotation-aware bean definition
     * @return the bean name, or {@code null} if none is found
     */
    protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
        // 获得类或者方法上所有的Annotation
        AnnotationMetadata amd = annotatedDef.getMetadata();
        // 得到所有annotation的类名
        Set<String> types = amd.getAnnotationTypes();
        String beanName = null;
        for (String type : types) {
            // 把annotation里面的字段与value,解读出来成map,字段名是key,value为value
            AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
            // 判断annotation是否有效,是否存在作为beanName的字段有value
            if (isStereotypeWithNameValue(type, amd.getMetaAnnotationTypes(type), attributes)) {
                // 从注解中获得value字段的值
                Object value = attributes.get("value");
                if (value instanceof String) {
                    String strVal = (String) value;
                    if (StringUtils.hasLength(strVal)) {
                        if (beanName != null && !strVal.equals(beanName)) {
                            throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
                                    "component names: '" + beanName + "' versus '" + strVal + "'");
                        }
                        beanName = strVal;
                    }
                }
            }
        }
        return beanName;
    }

    /**
     * Check whether the given annotation is a stereotype that is allowed
     * to suggest a component name through its annotation {@code value()}.
     *
     * @param annotationType      the name of the annotation class to check
     * @param metaAnnotationTypes the names of meta-annotations on the given annotation
     * @param attributes          the map of attributes for the given annotation
     * @return whether the annotation qualifies as a stereotype with component name
     */
    protected boolean isStereotypeWithNameValue(String annotationType,
                                                Set<String> metaAnnotationTypes, Map<String, Object> attributes) {

        boolean isStereotype = annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME) ||
                (metaAnnotationTypes != null && metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME)) ||
                annotationType.equals("javax.annotation.ManagedBean") ||
                annotationType.equals("javax.inject.Named");

        return (isStereotype && attributes != null && attributes.containsKey("value"));
    }

    /**
     * Derive a default bean name from the given bean definition.
     * <p>The default implementation delegates to {@link #buildDefaultBeanName(BeanDefinition)}.
     *
     * @param definition the bean definition to build a bean name for
     * @param registry   the registry that the given bean definition is being registered with
     * @return the default bean name (never {@code null})
     */
    protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        return buildDefaultBeanName(definition);
    }

    /**
     * Derive a default bean name from the given bean definition.
     * <p>The default implementation simply builds a decapitalized version
     * of the short class name: e.g. "mypackage.MyJdbcDao" -> "myJdbcDao".
     * <p>Note that inner classes will thus have names of the form
     * "outerClassName.InnerClassName", which because of the period in the
     * name may be an issue if you are autowiring by name.
     *
     * @param definition the bean definition to build a bean name for
     * @return the default bean name (never {@code null})
     */
    protected String buildDefaultBeanName(BeanDefinition definition) {
        String shortClassName = ClassUtils.getShortName(definition.getBeanClassName());
        return Introspector.decapitalize(shortClassName);
    }

}
