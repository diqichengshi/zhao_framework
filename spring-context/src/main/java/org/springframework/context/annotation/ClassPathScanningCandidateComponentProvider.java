package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ClassPathScanningCandidateComponentProvider implements EnvironmentCapable, ResourceLoaderAware {
    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    protected final Log logger = LogFactory.getLog(getClass());

    private Environment environment;

    private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

    private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private MetadataReaderFactory metadataReaderFactory =
            new CachingMetadataReaderFactory(this.resourcePatternResolver);

    private final List<TypeFilter> includeFilters = new LinkedList<TypeFilter>();

    private final List<TypeFilter> excludeFilters = new LinkedList<TypeFilter>();

    /**
     * 初始化时设置注解过滤器
     * Register the default filter for {@link Component @Component}.
     * <p>This will implicitly register all annotations that have the
     * {@link Component @Component} meta-annotation including the
     * {@link Repository @Repository}, {@link Service @Service}, and
     * {@link Controller @Controller} stereotype annotations.
     * <p>Also supports Java EE 6's {@link javax.annotation.ManagedBean} and
     * JSR-330's {@link javax.inject.Named} annotations, if available.
     */
    public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters, Environment environment) {
        if (useDefaultFilters) {
            registerDefaultFilters();
        }
        Assert.notNull(environment, "Environment must not be null");
        this.environment = environment;
    }


    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
    }

    /**
     * Set the Environment to use when resolving placeholders and evaluating
     * {@link Conditional @Conditional}-annotated component classes.
     * <p>The default is a {@link StandardEnvironment}.
     *
     * @param environment the Environment to use
     */
    public void setEnvironment(Environment environment) {
        Assert.notNull(environment, "Environment must not be null");
        this.environment = environment;
    }


    @Override
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Set the resource pattern to use when scanning the classpath.
     * This value will be appended to each base package name.
     *
     * @see #findCandidateComponents(String)
     * @see #DEFAULT_RESOURCE_PATTERN
     */
    public void setResourcePattern(String resourcePattern) {
        Assert.notNull(resourcePattern, "'resourcePattern' must not be null");
        this.resourcePattern = resourcePattern;
    }

    public void addIncludeFilter(TypeFilter includeFilter) {
        this.includeFilters.add(includeFilter);
    }

    public void addExcludeFilter(TypeFilter excludeFilter) {
        this.excludeFilters.add(0, excludeFilter);
    }

    /**
     * 初始化时设置注解过滤器
     */
    protected void registerDefaultFilters() {
        // 添加Component类型
        this.includeFilters.add(new AnnotationTypeFilter(Component.class));
        ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();
        try {
            this.includeFilters.add(new AnnotationTypeFilter(
                    ((Class<? extends Annotation>) ClassUtils.forName("javax.annotation.ManagedBean", cl)), false));
            logger.debug("JSR-250 'javax.annotation.ManagedBean' found and supported for component scanning");
        } catch (ClassNotFoundException ex) {
            // JSR-250 1.1 API (as included in Java EE 6) not available - simply skip.
        }
        try {
            this.includeFilters.add(new AnnotationTypeFilter(
                    ((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Named", cl)), false));
            logger.debug("JSR-330 'javax.inject.Named' annotation found and supported for component scanning");
        } catch (ClassNotFoundException ex) {
            // JSR-330 API not available - simply skip.
        }
    }

    /**
     * 扫描包下的class文件，把有Component注解的封装BeanDefinition列表返回
     * Scan the class path for candidate components.
     *
     * @param basePackage the package to check for annotated classes
     * @return a corresponding Set of autodetected bean definitions
     */
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
        try {
            // classpath*:basePackage/**/*.class
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    resolveBasePackage(basePackage) + "/" + this.resourcePattern;
            // 获取basePackage包下的 .class 文件资源
            Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {

                // 判断是否可读
                if (resource.isReadable()) {
                    try {
                        // 获取.class文件类信息
                        MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
                        if (isCandidateComponent(metadataReader)) {
                            ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                            // sbd.setResource(resource);
                            // sbd.setSource(resource);
                            if (isCandidateComponent(sbd)) {
                                candidates.add(sbd);
                            }
                        } // end if
                    } catch (Throwable ex) {
                        throw new BeanDefinitionStoreException(
                                "Failed to read candidate component class: " + resource, ex);
                    }
                } // end if
            }
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
        }
        return candidates;
    }

    protected String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(this.environment.resolveRequiredPlaceholders(basePackage));
    }

    protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
        for (TypeFilter tf : this.excludeFilters) {
            if (tf.match(metadataReader, this.metadataReaderFactory)) {
                return false;
            }
        }
        for (TypeFilter tf : this.includeFilters) {
            if (tf.match(metadataReader, this.metadataReaderFactory)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return (beanDefinition.getMetadata().isConcrete() && beanDefinition.getMetadata().isIndependent());
    }

    /**
     * Return the ResourceLoader that this component provider uses.
     */
    public final ResourceLoader getResourceLoader() {
        return this.resourcePatternResolver;
    }
}
