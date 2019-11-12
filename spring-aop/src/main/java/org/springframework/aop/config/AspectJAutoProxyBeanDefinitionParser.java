package org.springframework.aop.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
/**
 * {@link BeanDefinitionParser} for the {@code aspectj-autoproxy} tag,
 * enabling the automatic application of @AspectJ-style aspects found in
 * the {@link org.springframework.beans.factory.BeanFactory}.
 * AspectJAutoProxyBeanDefinitionParser主要的功能就是将AnnotationAwareAspectJAutoProxyCreator注册到Spring容器中,把bean交给Spring去托管。
 * AnnotationAwareAspectJAutoProxyCreator的功能我们大胆猜测一下:应该也就是生成对象的代理类的相关功能,这个我们接下来再看。
 * 那么问题来了,我们最开始的类AopNamespaceHandler.init()方法是在什么时候被调用的呢?
 * 什么时候生效的? 这个决定了我们注册到Spring的AnnotationAwareAspectJAutoProxyCreator的生效时间?
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AspectJAutoProxyBeanDefinitionParser implements BeanDefinitionParser {
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
        extendBeanDefinition(element, parserContext); // 对子标签<aop:include/>的解析
        return null;
    }

    private void extendBeanDefinition(Element element, ParserContext parserContext) {
        BeanDefinition beanDef =
                parserContext.getRegistry().getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
        if (element.hasChildNodes()) {
            addIncludePatterns(element, parserContext, beanDef);
        }
    }

    private void addIncludePatterns(Element element, ParserContext parserContext, BeanDefinition beanDef) {
        ManagedList<TypedStringValue> includePatterns = new ManagedList<TypedStringValue>();
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                Element includeElement = (Element) node;
                TypedStringValue valueHolder = new TypedStringValue(includeElement.getAttribute("name"));
                valueHolder.setSource(parserContext.extractSource(includeElement));
                includePatterns.add(valueHolder);
            }
        }
        if (!includePatterns.isEmpty()) {
            includePatterns.setSource(parserContext.extractSource(element));
            beanDef.getPropertyValues().add("includePatterns", includePatterns);
        }
    }

}
