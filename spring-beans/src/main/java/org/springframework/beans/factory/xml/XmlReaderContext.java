package org.springframework.beans.factory.xml;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class XmlReaderContext extends ReaderContext {

    private final XmlBeanDefinitionReader reader;

    private final NamespaceHandlerResolver namespaceHandlerResolver;

    public XmlReaderContext(Resource resource, ProblemReporter problemReporter,
                            ReaderEventListener eventListener, SourceExtractor sourceExtractor,
                            XmlBeanDefinitionReader reader, NamespaceHandlerResolver namespaceHandlerResolver) {
        super(resource, problemReporter, eventListener, sourceExtractor);
        this.reader = reader;
        this.namespaceHandlerResolver = namespaceHandlerResolver;
    }

    public NamespaceHandlerResolver getNamespaceHandlerResolver() {
        return namespaceHandlerResolver;
    }

    public final XmlBeanDefinitionReader getReader() {
        return this.reader;
    }

    public final Environment getEnvironment() {
        return this.reader.getEnvironment();
    }

    public final BeanDefinitionRegistry getRegistry() {
        return this.reader.getRegistry();
    }

    public final ResourceLoader getResourceLoader() {
        return this.reader.getResourceLoader();
    }

    public final ClassLoader getBeanClassLoader() {
        return this.reader.getBeanClassLoader();
    }

    public String generateBeanName(BeanDefinition beanDefinition) {
        return this.reader.getBeanNameGenerator().generateBeanName(beanDefinition, getRegistry());
    }

}
