package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.w3c.dom.Node;

public interface BeanDefinitionDecorator {
    /**
     * Parse the specified {@link Node} (either an element or an attribute) and decorate
     * the supplied {@link org.springframework.beans.factory.config.BeanDefinition},
     * returning the decorated definition.
     * <p>Implementations may choose to return a completely new definition, which will
     * replace the original definition in the resulting
     * {@link org.springframework.beans.factory.BeanFactory}.
     * <p>The supplied {@link ParserContext} can be used to register any additional
     * beans needed to support the main definition.
     */
    BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext);
}
