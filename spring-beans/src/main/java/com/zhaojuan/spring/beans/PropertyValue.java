package com.zhaojuan.spring.beans;

import com.zhaojuan.spring.core.util.Assert;

public class PropertyValue {
    private final String name;

    private final Object value;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public PropertyValue(PropertyValue original) {
        Assert.notNull(original, "Original must not be null");
        this.name = original.getName();
        this.value = original.getValue();
    }

    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return this.value;
    }
}
