package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;
import org.springframework.beans.config.BeanDefinition;

public interface BeanDefinitionParser {

    BeanDefinition parse(Element element, ParserContext parserContext);

}