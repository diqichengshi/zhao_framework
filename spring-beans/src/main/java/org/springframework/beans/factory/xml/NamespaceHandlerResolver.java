package org.springframework.beans.factory.xml;

public interface NamespaceHandlerResolver {
    /**
     * Resolve the namespace URI and return the located {@link NamespaceHandler}
     * implementation.
     * @param namespaceUri the relevant namespace URI
     * @return the located {@link NamespaceHandler} (may be {@code null})
     */
    NamespaceHandler resolve(String namespaceUri);
}
