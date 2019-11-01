package org.springframework.beans.factory.parsing;

import java.util.EventListener;

public interface ReaderEventListener extends EventListener {

    /**
     * Notification that the given defaults has been registered.
     * @param defaultsDefinition a descriptor for the defaults
     * @see org.springframework.beans.factory.xml.DocumentDefaultsDefinition
     */
    void defaultsRegistered(DefaultsDefinition defaultsDefinition);

    /**
     * Notification that the given component has been registered.
     * @param componentDefinition a descriptor for the new component
     * @see BeanComponentDefinition
     */
    void componentRegistered(ComponentDefinition componentDefinition);

    /**
     * Notification that the given import has been processed.
     * @param importDefinition a descriptor for the import
     */
    void importProcessed(ImportDefinition importDefinition);
}
