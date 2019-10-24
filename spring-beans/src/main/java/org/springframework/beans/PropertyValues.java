package org.springframework.beans;

import java.util.List;

public interface PropertyValues {

    PropertyValue[] getPropertyValues();

    PropertyValue getPropertyValue(String propertyName);

    boolean contains(String propertyName);

    boolean isEmpty();

    List<PropertyValue> getPropertyValueList();
}
