package org.springframework.beans.config;

import org.springframework.beans.factory.ObjectFactory;

public interface Scope {
    Object get(String name, ObjectFactory<?> objectFactory);

    Object resolveContextualObject(String key);

    void registerDestructionCallback(String name, Runnable callback);

}
