package org.springframework.context.annotation;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import java.util.LinkedHashSet;
import java.util.Set;

public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {
    private final BeanDefinitionRegistry registry;

    private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

    private String[] autowireCandidatePatterns;

    private boolean includeAnnotationConfig = true;

    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();


    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        this(registry, true);
    }

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
    }

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment) {
        // 初始化时设置注解过滤器
        super(useDefaultFilters, environment);

        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        this.registry = registry;

        // Determine ResourceLoader to use.
        if (this.registry instanceof ResourceLoader) {
            // setResourceLoader((ResourceLoader) this.registry);
        }
    }

    private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        if (registry instanceof EnvironmentCapable) {
            return ((EnvironmentCapable) registry).getEnvironment();
        }
        return new StandardEnvironment();
    }

    /**
     * Perform a scan within the specified base packages.
     *
     * @param basePackages the packages to check for annotated classes
     * @return number of beans registered
     */
    public int scan(String... basePackages) {
        int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

        doScan(basePackages);

        // Register annotation config processors, if necessary.
        if (this.includeAnnotationConfig) {
            AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
        }

        return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
    }

    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Assert.notEmpty(basePackages, "At least one base package must be specified");
        Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<BeanDefinitionHolder>();
        for (String basePackage : basePackages) {
            // 扫描包下有Spring Component注解,并且生成BeanDefinition
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                // // 设置scope,默认是singleton
                ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
                candidate.setScope(scopeMetadata.getScopeName());
                String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
                if (candidate instanceof AbstractBeanDefinition) {
                    postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
                }
                if (candidate instanceof AnnotatedBeanDefinition) {
                    AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
                }
                if (checkCandidate(beanName, candidate)) {
                    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                    // definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                    beanDefinitions.add(definitionHolder);
                    // 注册到Spring容器
                    registerBeanDefinition(definitionHolder, this.registry);
                }
            }
        }
        return beanDefinitions;
    }

    protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
        beanDefinition.applyDefaults(this.beanDefinitionDefaults);
       /* if (this.autowireCandidatePatterns != null) {
            beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
        }*/
    }

    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
    }

    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        if (!this.registry.containsBeanDefinition(beanName)) {
            return true;
        }
        BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
        /*BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
        if (originatingDef != null) {
            existingDef = originatingDef;
        }*/
        if (isCompatible(beanDefinition, existingDef)) {
            return false;
        }
        throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
                "' for bean class [" + beanDefinition.getBeanName() + "] conflicts with existing, " +
                "non-compatible bean definition of same name and class [" + existingDef.getBeanName() + "]");
    }

    protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
        return !newDefinition.equals(existingDefinition);  // scanned equivalent class twice
    }
}
