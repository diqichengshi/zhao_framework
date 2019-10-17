package com.zhaojuan.spring.beans.config;

import com.zhaojuan.spring.beans.factory.ObjectFactory;

public interface Scope {
    Object get(String name, ObjectFactory<?> objectFactory);
}
