package org.springframework.beans;

import org.springframework.util.Assert;

import java.io.Serializable;

public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {
    private final String name;

    private final Object value;

    private Object source;

    private boolean optional = false;

    private boolean converted = false;

    private Object convertedValue;

    volatile Boolean conversionNecessary;

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

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isOptional() {
        return this.optional;
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

    /**
     * Return the converted value of the constructor argument,
     * after processed type conversion.
     */
    public synchronized Object getConvertedValue() {
        return this.convertedValue;
    }

    /**
     * Return the original PropertyValue instance for this value holder.
     * @return the original PropertyValue (either a source of this
     * value holder or this value holder itself).
     */
    public PropertyValue getOriginalPropertyValue() {
        PropertyValue original = this;
        while (original.source instanceof PropertyValue && original.source != original) {
            original = (PropertyValue) original.source;
        }
        return original;
    }

}
