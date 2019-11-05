package org.springframework.beans.factory.xml;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

public abstract class NamespaceHandlerSupport implements NamespaceHandler {
    private final Map<String, BeanDefinitionParser> parsers =
            new HashMap<String, BeanDefinitionParser>();

    private final Map<String, BeanDefinitionDecorator> decorators =
            new HashMap<String, BeanDefinitionDecorator>();

    private final Map<String, BeanDefinitionDecorator> attributeDecorators =
            new HashMap<String, BeanDefinitionDecorator>();

    protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
        this.parsers.put(elementName, parser);
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return findParserForElement(element, parserContext).parse(element, parserContext);
    }

    private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
        String localName = parserContext.getDelegate().getLocalName(element);
        BeanDefinitionParser parser = this.parsers.get(localName);
        if (parser == null) {
            parserContext.getReaderContext().fatal(
                    "Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
        }
        return parser;
    }

    /**
     * Decorates the supplied {@link Node} by delegating to the {@link BeanDefinitionDecorator} that
     * is registered to handle that {@link Node}.
     */
    @Override
    public BeanDefinitionHolder decorate(
            Node node, BeanDefinitionHolder definition, ParserContext parserContext) {

        return findDecoratorForNode(node, parserContext).decorate(node, definition, parserContext);
    }


    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using
     * the local name of the supplied {@link Node}. Supports both {@link Element Elements}
     * and {@link Attr Attrs}.
     */
    private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
        BeanDefinitionDecorator decorator = null;
        String localName = parserContext.getDelegate().getLocalName(node);
        if (node instanceof Element) {
            decorator = this.decorators.get(localName);
        } else if (node instanceof Attr) {
            decorator = this.attributeDecorators.get(localName);
        } else {
            parserContext.getReaderContext().fatal(
                    "Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
        }
        if (decorator == null) {
            parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " +
                    (node instanceof Element ? "element" : "attribute") + " [" + localName + "]", node);
        }
        return decorator;
    }

}
