package org.springframework.beans;

import org.springframework.util.Assert;

public class PropertyValue {
    private final String name;

    private final Object value;

    private boolean converted = false;

    private Object convertedValue;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public PropertyValue(PropertyValue original) {
        Assert.notNull(original, "Original must not be null");
        this.name = original.getName();
        this.value = original.getValue();
    }

    /**
     * Constructor that exposes a new value for an original value holder.
     * The original holder will be exposed as source of the new holder.
     * @param original the PropertyValue to link to (never {@code null})
     * @param newValue the new value to apply
     */
    public PropertyValue(PropertyValue original, Object newValue) {
        Assert.notNull(original, "Original must not be null");
        this.name = original.getName();
        this.value = newValue;
    }
    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return this.value;
    }


    /**
     * Return whether this holder contains a converted value already ({@code true}),
     * or whether the value still needs to be converted ({@code false}).
     */
    public synchronized boolean isConverted() {
        return this.converted;
    }
    /**
     * Set the converted value of the constructor argument,
     * after processed type conversion.
     */
    public synchronized void setConvertedValue(Object value) {
        this.converted = true;
        this.convertedValue = value;
    }
}
