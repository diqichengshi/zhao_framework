package org.springframework.aop.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;

public class AopNamespaceUtils {
    /**
     * The {@code proxy-target-class} attribute as found on AOP-related XML tags.
     */
    public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

    /**
     * The {@code expose-proxy} attribute as found on AOP-related XML tags.
     */
    private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";

    /**
     * 注册的是InfrastructureAdvisorAutoProxyCreator
     */
    public static void registerAutoProxyCreatorIfNecessary(
            ParserContext parserContext, Element sourceElement) {
        // 注意:区分 @AspectJ:@AspectJ注册的是AnnotationAwareAspectJAutoProxyCreator
        // @Transaction注册的是InfrastructureAdvisorAutoProxyCreator类
        BeanDefinition beanDefinition = AopConfigUtils.registerAutoProxyCreatorIfNecessary(
                parserContext.getRegistry(), parserContext.extractSource(sourceElement));
        useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
        registerComponentIfNecessary(beanDefinition, parserContext); // 注册Component
    }

    /**
     * 注册AspectJAwareAdvisorAutoProxyCreator类
     */
    public static void registerAspectJAutoProxyCreatorIfNecessary(
            ParserContext parserContext, Element sourceElement) {
        // TODO 注册AspectJAwareAdvisorAutoProxyCreator类
        BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(
                parserContext.getRegistry(), parserContext.extractSource(sourceElement));
        useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
        registerComponentIfNecessary(beanDefinition, parserContext); // 注册Component
    }

    /**
     * 主要就是为了注册AnnotationAwareAspectJAutoProxyCreator类
     */
    public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
            ParserContext parserContext, Element sourceElement) {
        // TODO 注册AnnotationAwareAspectJAutoProxyCreator类
        BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
                parserContext.getRegistry(), parserContext.extractSource(sourceElement));
        useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);

        registerComponentIfNecessary(beanDefinition, parserContext); // 注册Component
    }

    /**
     * 设置proxy-target-class和expose-proxy
     */
    private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, Element sourceElement) {
        if (sourceElement != null) {
            boolean proxyTargetClass = Boolean.valueOf(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
            if (proxyTargetClass) {
                AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
            }
            boolean exposeProxy = Boolean.valueOf(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
            if (exposeProxy) {
                AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
            }
        }
    }

    private static void registerComponentIfNecessary(BeanDefinition beanDefinition, ParserContext parserContext) {
        if (beanDefinition != null) {
            BeanComponentDefinition componentDefinition =
                    new BeanComponentDefinition(beanDefinition, AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
            parserContext.registerComponent(componentDefinition);
        }
    }
}
