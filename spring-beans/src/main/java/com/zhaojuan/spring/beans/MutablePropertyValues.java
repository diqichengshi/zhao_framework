package com.zhaojuan.spring.beans;

import org.springframework.util.StringUtils;

import java.util.*;

public class MutablePropertyValues implements PropertyValues {

    private final List<PropertyValue> propertyValueList;

    private Set<String> processedProperties;

    public MutablePropertyValues() {
        this.propertyValueList = new ArrayList<PropertyValue>(0);
    }

    public MutablePropertyValues(Map<?, ?> original) {
        // We can optimize this because it's all new:
        // There is no replacement of existing property values.
        if (original != null) {
            this.propertyValueList = new ArrayList<PropertyValue>(original.size());
            for (Map.Entry<?, ?> entry : original.entrySet()) {
                this.propertyValueList.add(new PropertyValue(entry.getKey().toString(), entry.getValue()));
            }
        } else {
            this.propertyValueList = new ArrayList<PropertyValue>(0);
        }
    }

    public MutablePropertyValues(List<PropertyValue> propertyValueList) {
        this.propertyValueList =
                (propertyValueList != null ? propertyValueList : new ArrayList<PropertyValue>());
    }

    public MutablePropertyValues(PropertyValues original) {
        // We can optimize this because it's all new:
        // There is no replacement of existing property values.
        if (original != null) {
            PropertyValue[] pvs = original.getPropertyValues();
            this.propertyValueList = new ArrayList<PropertyValue>(pvs.length);
            for (PropertyValue pv : pvs) {
                this.propertyValueList.add(new PropertyValue(pv));
            }
        }
        else {
            this.propertyValueList = new ArrayList<PropertyValue>(0);
        }
    }

    public List<PropertyValue> getPropertyValueList() {
        return this.propertyValueList;
    }

    public int size() {
        return this.propertyValueList.size();
    }

    public MutablePropertyValues addPropertyValues(PropertyValues other) {
        if (other != null) {
            PropertyValue[] pvs = other.getPropertyValues();
            for (PropertyValue pv : pvs) {
                addPropertyValue(new PropertyValue(pv));
            }
        }
        return this;
    }

    public MutablePropertyValues addPropertyValues(Map<?, ?> other) {
        if (other != null) {
            for (Map.Entry<?, ?> entry : other.entrySet()) {
                addPropertyValue(new PropertyValue(entry.getKey().toString(), entry.getValue()));
            }
        }
        return this;
    }

    public MutablePropertyValues addPropertyValue(PropertyValue pv) {
        for (int i = 0; i < this.propertyValueList.size(); i++) {
            PropertyValue currentPv = this.propertyValueList.get(i);
            if (currentPv.getName().equals(pv.getName())) {
                setPropertyValueAt(pv, i);
                return this;
            }
        }
        this.propertyValueList.add(pv);
        return this;
    }

    /**
     * Overloaded version of {@code addPropertyValue} that takes
     * a property name and a property value.
     * <p>Note: As of Spring 3.0, we recommend using the more concise
     * and chaining-capable variant {@link #add}.
     *
     * @param propertyName  name of the property
     * @param propertyValue value of the property
     * @see #addPropertyValue(PropertyValue)
     */
    public void addPropertyValue(String propertyName, Object propertyValue) {
        addPropertyValue(new PropertyValue(propertyName, propertyValue));
    }

    /**
     * Add a PropertyValue object, replacing any existing one for the
     * corresponding property or getting merged with it (if applicable).
     *
     * @param propertyName  name of the property
     * @param propertyValue value of the property
     * @return this in order to allow for adding multiple property values in a chain
     */
    public MutablePropertyValues add(String propertyName, Object propertyValue) {
        addPropertyValue(new PropertyValue(propertyName, propertyValue));
        return this;
    }

    /**
     * Modify a PropertyValue object held in this object.
     * Indexed from 0.
     */
    public void setPropertyValueAt(PropertyValue pv, int i) {
        this.propertyValueList.set(i, pv);
    }


    /**
     * Remove the given PropertyValue, if contained.
     *
     * @param pv the PropertyValue to remove
     */
    public void removePropertyValue(PropertyValue pv) {
        this.propertyValueList.remove(pv);
    }

    /**
     * Overloaded version of {@code removePropertyValue} that takes a property name.
     *
     * @param propertyName name of the property
     * @see #removePropertyValue(PropertyValue)
     */
    public void removePropertyValue(String propertyName) {
        this.propertyValueList.remove(getPropertyValue(propertyName));
    }


    @Override
    public PropertyValue[] getPropertyValues() {
        return this.propertyValueList.toArray(new PropertyValue[this.propertyValueList.size()]);
    }

    @Override
    public PropertyValue getPropertyValue(String propertyName) {
        for (PropertyValue pv : this.propertyValueList) {
            if (pv.getName().equals(propertyName)) {
                return pv;
            }
        }
        return null;
    }

    /**
     * Get the raw property value, if any.
     *
     * @param propertyName the name to search for
     * @return the raw property value, or {@code null}
     * @see #getPropertyValue(String)
     * @see PropertyValue#getValue()
     * @since 4.0
     */
    public Object get(String propertyName) {
        PropertyValue pv = getPropertyValue(propertyName);
        return (pv != null ? pv.getValue() : null);
    }

    @Override
    public boolean contains(String propertyName) {
        return (getPropertyValue(propertyName) != null ||
                (this.processedProperties != null && this.processedProperties.contains(propertyName)));
    }

    @Override
    public boolean isEmpty() {
        return this.propertyValueList.isEmpty();
    }


    /**
     * Register the specified property as "processed" in the sense
     * of some processor calling the corresponding setter method
     * outside of the PropertyValue(s) mechanism.
     * <p>This will lead to {@code true} being returned from
     * a {@link #contains} call for the specified property.
     *
     * @param propertyName the name of the property.
     */
    public void registerProcessedProperty(String propertyName) {
        if (this.processedProperties == null) {
            this.processedProperties = new HashSet<String>();
        }
        this.processedProperties.add(propertyName);
    }

    /**
     * Clear the "processed" registration of the given property, if any.
     *
     * @since 3.2.13
     */
    public void clearProcessedProperty(String propertyName) {
        if (this.processedProperties != null) {
            this.processedProperties.remove(propertyName);
        }
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MutablePropertyValues)) {
            return false;
        }
        MutablePropertyValues that = (MutablePropertyValues) other;
        return this.propertyValueList.equals(that.propertyValueList);
    }

    @Override
    public int hashCode() {
        return this.propertyValueList.hashCode();
    }

    @Override
    public String toString() {
        PropertyValue[] pvs = getPropertyValues();
        StringBuilder sb = new StringBuilder("PropertyValues: length=").append(pvs.length);
        if (pvs.length > 0) {
            sb.append("; ").append(StringUtils.arrayToDelimitedString(pvs, "; "));
        }
        return sb.toString();
    }

}
