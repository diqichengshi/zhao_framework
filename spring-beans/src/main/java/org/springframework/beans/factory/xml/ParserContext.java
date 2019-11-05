package org.springframework.beans.factory.xml;

import org.springframework.beans.config.BeanDefinition;

public class ParserContext {
    private final XmlReaderContext readerContext;

    private final BeanDefinitionParserDelegate delegate;

    private BeanDefinition containingBeanDefinition;

    public ParserContext(XmlReaderContext readerContext, BeanDefinitionParserDelegate delegate,
                         BeanDefinition containingBeanDefinition) {

        this.readerContext = readerContext;
        this.delegate = delegate;
        this.containingBeanDefinition = containingBeanDefinition;
    }

    public final XmlReaderContext getReaderContext() {
        return this.readerContext;
    }

    public final BeanDefinitionParserDelegate getDelegate() {
        return this.delegate;
    }

    public Object extractSource(Object sourceCandidate) {
        return this.readerContext.extractSource(sourceCandidate);
    }

}
