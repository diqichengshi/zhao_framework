package org.springframework.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.exception.*;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;
import java.util.*;

public abstract class AbstractNestablePropertyAccessor extends AbstractPropertyAccessor {

    private static final Log logger = LogFactory.getLog(AbstractNestablePropertyAccessor.class);

    private int autoGrowCollectionLimit = Integer.MAX_VALUE;

    /**
     * 被包装的对账
     */
    private Object object;

    private String nestedPath = "";

    private Object rootObject;

    TypeConverterDelegate typeConverterDelegate;

    private Map<String, AbstractNestablePropertyAccessor> nestedPropertyAccessors;

    public AbstractNestablePropertyAccessor(Object object) {
        this.object = object;
        setWrappedInstance(object);
        Assert.notNull(object, "target object must not be null");
    }

    /**
     * Create a new accessor for the given object,
     * registering a nested path that the object is in.
     *
     * @param object     object wrapped by this accessor
     * @param nestedPath the nested path of the object
     * @param parent     the containing accessor (must not be {@code null})
     */
    protected AbstractNestablePropertyAccessor(Object object, String nestedPath, AbstractNestablePropertyAccessor parent) {
        setWrappedInstance(object, nestedPath, parent.getWrappedInstance());
        setExtractOldValueForEditor(parent.isExtractOldValueForEditor());
        setAutoGrowNestedPaths(parent.isAutoGrowNestedPaths());
        setAutoGrowCollectionLimit(parent.getAutoGrowCollectionLimit());
        setConversionService(parent.getConversionService());
    }

    /**
     * Specify a limit for array and collection auto-growing.
     * <p>Default is unlimited on a plain accessor.
     */
    public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
        this.autoGrowCollectionLimit = autoGrowCollectionLimit;
    }

    public int getAutoGrowCollectionLimit() {
        return this.autoGrowCollectionLimit;
    }

    public void setWrappedInstance(Object object) {
        setWrappedInstance(object, "", null);
    }

    public void setWrappedInstance(Object object, String nestedPath, Object rootObject) {
        Assert.notNull(object, "Target object must not be null");
        this.object = object;
        this.nestedPath = (nestedPath != null ? nestedPath : "");
        this.rootObject = (!"".equals(this.nestedPath) ? rootObject : this.object);
        this.typeConverterDelegate = new TypeConverterDelegate(this, this.object);

    }

    public Object getWrappedInstance() {
        return this.object;
    }

    public Class<?> getWrappedClass() {
        return this.object.getClass();
    }


    public Class<?> getRootClass() {
        return (this.rootObject != null ? this.rootObject.getClass() : null);
    }


    public final String getNestedPath() {
        return this.nestedPath;
    }

    @Override
    public void setPropertyValue(String propertyName, Object value) throws BeansException {
        AbstractNestablePropertyAccessor nestedPa;
        try {
            nestedPa = getPropertyAccessorForPropertyPath(propertyName);
        } catch (NotReadablePropertyException ex) {
            throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Nested property in path '" + propertyName + "' does not exist", ex);
        }
        PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
        nestedPa.setPropertyValue(tokens, new PropertyValue(propertyName, value));
    }

    @Override
    public void setPropertyValue(PropertyValue pv) throws BeansException {
        PropertyTokenHolder tokens = (PropertyTokenHolder) pv.resolvedTokens;
        if (tokens == null) {
            String propertyName = pv.getName();
            AbstractNestablePropertyAccessor nestedPa;
            try {
                nestedPa = getPropertyAccessorForPropertyPath(propertyName);
            } catch (NotReadablePropertyException ex) {
                throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
                        "Nested property in path '" + propertyName + "' does not exist", ex);
            }
            tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
            if (nestedPa == this) {
                pv.getOriginalPropertyValue().resolvedTokens = tokens;
            }
            nestedPa.setPropertyValue(tokens, pv);
        } else {
            setPropertyValue(tokens, pv);
        }
    }

    @SuppressWarnings("unchecked")
    protected void setPropertyValue(PropertyTokenHolder tokens, PropertyValue pv) throws BeansException {
        String propertyName = tokens.canonicalName;
        String actualName = tokens.actualName;

        if (tokens.keys != null) {
            // Apply indexes and map keys: fetch value for all keys but the last one.
            PropertyTokenHolder getterTokens = new PropertyTokenHolder();
            getterTokens.canonicalName = tokens.canonicalName;
            getterTokens.actualName = tokens.actualName;
            getterTokens.keys = new String[tokens.keys.length - 1];
            System.arraycopy(tokens.keys, 0, getterTokens.keys, 0, tokens.keys.length - 1);
            Object propValue;
            try {
                propValue = getPropertyValue(getterTokens);
            } catch (NotReadablePropertyException ex) {
                throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
                        "Cannot access indexed value in property referenced " +
                                "in indexed property path '" + propertyName + "'", ex);
            }
            // Set value for last key.
            String key = tokens.keys[tokens.keys.length - 1];
            if (propValue == null) {
                // null map value case
                if (isAutoGrowNestedPaths()) {
                    int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
                    getterTokens.canonicalName = tokens.canonicalName.substring(0, lastKeyIndex);
                    propValue = setDefaultValue(getterTokens);
                } else {
                    throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
                            "Cannot access indexed value in property referenced " +
                                    "in indexed property path '" + propertyName + "': returned null");
                }
            }
            if (propValue.getClass().isArray()) {
                PropertyHandler ph = getLocalPropertyHandler(actualName);
                Class<?> requiredType = propValue.getClass().getComponentType();
                int arrayIndex = Integer.parseInt(key);
                Object oldValue = null;
                try {
                    if (isExtractOldValueForEditor() && arrayIndex < Array.getLength(propValue)) {
                        oldValue = Array.get(propValue, arrayIndex);
                    }
                    Object convertedValue = convertIfNecessary(propertyName, oldValue, pv.getValue(),
                            requiredType, ph.nested(tokens.keys.length));
                    int length = Array.getLength(propValue);
                    if (arrayIndex >= length && arrayIndex < this.autoGrowCollectionLimit) {
                        Class<?> componentType = propValue.getClass().getComponentType();
                        Object newArray = Array.newInstance(componentType, arrayIndex + 1);
                        System.arraycopy(propValue, 0, newArray, 0, length);
                        setPropertyValue(actualName, newArray);
                        propValue = getPropertyValue(actualName);
                    }
                    Array.set(propValue, arrayIndex, convertedValue);
                } catch (IndexOutOfBoundsException ex) {
                    throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                            "Invalid array index in property path '" + propertyName + "'", ex);
                }
            } else if (propValue instanceof List) {
                PropertyHandler ph = getPropertyHandler(actualName);
                Class<?> requiredType = ph.getCollectionType(tokens.keys.length);
                List<Object> list = (List<Object>) propValue;
                int index = Integer.parseInt(key);
                Object oldValue = null;
                if (isExtractOldValueForEditor() && index < list.size()) {
                    oldValue = list.get(index);
                }
                Object convertedValue = convertIfNecessary(propertyName, oldValue, pv.getValue(),
                        requiredType, ph.nested(tokens.keys.length));
                int size = list.size();
                if (index >= size && index < this.autoGrowCollectionLimit) {
                    for (int i = size; i < index; i++) {
                        try {
                            list.add(null);
                        } catch (NullPointerException ex) {
                            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                                    "Cannot set element with index " + index + " in List of size " +
                                            size + ", accessed using property path '" + propertyName +
                                            "': List does not support filling up gaps with null elements");
                        }
                    }
                    list.add(convertedValue);
                } else {
                    try {
                        list.set(index, convertedValue);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                                "Invalid list index in property path '" + propertyName + "'", ex);
                    }
                }
            } else if (propValue instanceof Map) {
                PropertyHandler ph = getLocalPropertyHandler(actualName);
                Class<?> mapKeyType = ph.getMapKeyType(tokens.keys.length);
                Class<?> mapValueType = ph.getMapValueType(tokens.keys.length);
                Map<Object, Object> map = (Map<Object, Object>) propValue;
                // IMPORTANT: Do not pass full property name in here - property editors
                // must not kick in for map keys but rather only for map values.
                TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
                Object convertedMapKey = convertIfNecessary(null, null, key, mapKeyType, typeDescriptor);
                Object oldValue = null;
                if (isExtractOldValueForEditor()) {
                    oldValue = map.get(convertedMapKey);
                }
                // Pass full property name and old value in here, since we want full
                // conversion ability for map values.
                Object convertedMapValue = convertIfNecessary(propertyName, oldValue, pv.getValue(),
                        mapValueType, ph.nested(tokens.keys.length));
                map.put(convertedMapKey, convertedMapValue);
            } else {
                throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                        "Property referenced in indexed property path '" + propertyName +
                                "' is neither an array nor a List nor a Map; returned value was [" + propValue + "]");
            }
        } else {
            PropertyHandler ph = getLocalPropertyHandler(actualName);
            if (ph == null || !ph.isWritable()) {
                if (pv.isOptional()) {
                    return;
                } else {
                    throw createNotWritablePropertyException(propertyName);
                }
            }
            Object oldValue = null;
            try {
                Object originalValue = pv.getValue();
                Object valueToApply = originalValue;
                if (!Boolean.FALSE.equals(pv.conversionNecessary)) {
                    if (pv.isConverted()) {
                        valueToApply = pv.getConvertedValue();
                    } else {
                        if (isExtractOldValueForEditor() && ph.isReadable()) {
                            try {
                                oldValue = ph.getValue();
                            } catch (Exception ex) {
                                if (ex instanceof PrivilegedActionException) {
                                    ex = ((PrivilegedActionException) ex).getException();
                                }
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Could not read previous value of property '" +
                                            this.nestedPath + propertyName + "'", ex);
                                }
                            }
                        }
                        valueToApply = convertForProperty(
                                propertyName, oldValue, originalValue, ph.toTypeDescriptor());
                    }
                    pv.getOriginalPropertyValue().conversionNecessary = (valueToApply != originalValue);
                }
                ph.setValue(object, valueToApply);
            } catch (TypeMismatchException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {
                PropertyChangeEvent propertyChangeEvent =
                        new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, pv.getValue());
                if (ex.getTargetException() instanceof ClassCastException) {
                    throw new TypeMismatchException(propertyChangeEvent, ph.getPropertyType(), ex.getTargetException());
                } else {
                    Throwable cause = ex.getTargetException();
                    if (cause instanceof UndeclaredThrowableException) {
                        // May happen e.g. with Groovy-generated methods
                        cause = cause.getCause();
                    }
                    throw new MethodInvocationException(propertyChangeEvent, cause);
                }
            } catch (Exception ex) {
                PropertyChangeEvent pce =
                        new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, pv.getValue());
                throw new MethodInvocationException(pce, ex);
            }
        }
    }

    public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
        try {
            AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
            String finalPath = getFinalPath(nestedPa, propertyName);
            PropertyTokenHolder tokens = getPropertyNameTokens(finalPath);
            PropertyHandler ph = nestedPa.getLocalPropertyHandler(tokens.actualName);
            if (ph != null) {
                if (tokens.keys != null) {
                    if (ph.isReadable() || ph.isWritable()) {
                        return ph.nested(tokens.keys.length);
                    }
                } else {
                    if (ph.isReadable() || ph.isWritable()) {
                        return ph.toTypeDescriptor();
                    }
                }
            }
        } catch (InvalidPropertyException ex) {
            // Consider as not determinable.
        }
        return null;
    }

    public boolean isReadableProperty(String propertyName) {
        try {
            PropertyHandler ph = getPropertyHandler(propertyName);
            if (ph != null) {
                return ph.isReadable();
            } else {
                // Maybe an indexed/mapped property...
                getPropertyValue(propertyName);
                return true;
            }
        } catch (InvalidPropertyException ex) {
            // Cannot be evaluated, so can't be readable.
        }
        return false;
    }

    public boolean isWritableProperty(String propertyName) {
        try {
            PropertyHandler ph = getPropertyHandler(propertyName);
            if (ph != null) {
                return ph.isWritable();
            } else {
                // Maybe an indexed/mapped property...
                getPropertyValue(propertyName);
                return true;
            }
        } catch (InvalidPropertyException ex) {
            // Cannot be evaluated, so can't be writable.
        }
        return false;
    }

    protected Object convertForProperty(String propertyName, Object oldValue, Object newValue, TypeDescriptor td)
            throws TypeMismatchException {

        return convertIfNecessary(propertyName, oldValue, newValue, td.getType(), td);
    }

    private Object convertIfNecessary(String propertyName, Object oldValue, Object newValue, Class<?> requiredType,
                                      TypeDescriptor td) throws TypeMismatchException {
        try {
            return this.typeConverterDelegate.convertIfNecessary(propertyName, oldValue, newValue, requiredType, td);
        } catch (ConverterNotFoundException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new ConversionNotSupportedException(pce, td.getType(), ex);
        } catch (ConversionException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new TypeMismatchException(pce, requiredType, ex);
        } catch (IllegalStateException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new ConversionNotSupportedException(pce, requiredType, ex);
        } catch (IllegalArgumentException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new TypeMismatchException(pce, requiredType, ex);
        }
    }


    public Object getPropertyValue(String propertyName) throws BeansException {
        AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
        PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
        return nestedPa.getPropertyValue(tokens);
    }

    @SuppressWarnings("unchecked")
    protected Object getPropertyValue(PropertyTokenHolder tokens) throws BeansException {
        String propertyName = tokens.canonicalName;
        String actualName = tokens.actualName;
        PropertyHandler ph = getLocalPropertyHandler(actualName);
        if (ph == null || !ph.isReadable()) {
            throw new NotReadablePropertyException(getRootClass(), this.nestedPath + propertyName);
        }
        try {
            Object value = ph.getValue();
            if (tokens.keys != null) {
                if (value == null) {
                    if (isAutoGrowNestedPaths()) {
                        value = setDefaultValue(tokens.actualName);
                    } else {
                        throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
                                "Cannot access indexed value of property referenced in indexed " +
                                        "property path '" + propertyName + "': returned null");
                    }
                }
                String indexedPropertyName = tokens.actualName;
                // apply indexes and map keys
                for (int i = 0; i < tokens.keys.length; i++) {
                    String key = tokens.keys[i];
                    if (value == null) {
                        throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
                                "Cannot access indexed value of property referenced in indexed " +
                                        "property path '" + propertyName + "': returned null");
                    } else if (value.getClass().isArray()) {
                        int index = Integer.parseInt(key);
                        value = growArrayIfNecessary(value, index, indexedPropertyName);
                        value = Array.get(value, index);
                    } else if (value instanceof List) {
                        int index = Integer.parseInt(key);
                        List<Object> list = (List<Object>) value;
                        growCollectionIfNecessary(list, index, indexedPropertyName, ph, i + 1);
                        value = list.get(index);
                    } else if (value instanceof Set) {
                        // Apply index to Iterator in case of a Set.
                        Set<Object> set = (Set<Object>) value;
                        int index = Integer.parseInt(key);
                        if (index < 0 || index >= set.size()) {
                            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                                    "Cannot get element with index " + index + " from Set of size " +
                                            set.size() + ", accessed using property path '" + propertyName + "'");
                        }
                        Iterator<Object> it = set.iterator();
                        for (int j = 0; it.hasNext(); j++) {
                            Object elem = it.next();
                            if (j == index) {
                                value = elem;
                                break;
                            }
                        }
                    } else if (value instanceof Map) {
                        Map<Object, Object> map = (Map<Object, Object>) value;
                        Class<?> mapKeyType = ph.getResolvableType().getNested(i + 1).asMap().resolveGeneric(0);
                        // IMPORTANT: Do not pass full property name in here - property editors
                        // must not kick in for map keys but rather only for map values.
                        TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
                        Object convertedMapKey = convertIfNecessary(null, null, key, mapKeyType, typeDescriptor);
                        value = map.get(convertedMapKey);
                    } else {
                        throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                                "Property referenced in indexed property path '" + propertyName +
                                        "' is neither an array nor a List nor a Set nor a Map; returned value was [" + value + "]");
                    }
                    indexedPropertyName += PROPERTY_KEY_PREFIX + key + PROPERTY_KEY_SUFFIX;
                }
            }
            return value;
        } catch (IndexOutOfBoundsException ex) {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Index of out of bounds in property path '" + propertyName + "'", ex);
        } catch (NumberFormatException ex) {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Invalid index in property path '" + propertyName + "'", ex);
        } catch (TypeMismatchException ex) {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Invalid index in property path '" + propertyName + "'", ex);
        } catch (InvocationTargetException ex) {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Getter for property '" + actualName + "' threw exception", ex);
        } catch (Exception ex) {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Illegal attempt to get property '" + actualName + "' threw exception", ex);
        }
    }

    /**
     * Return the {@link PropertyHandler} for the specified {@code propertyName}, navigating
     * if necessary. Return {@code null} if not found rather than throwing an exception.
     *
     * @param propertyName the property to obtain the descriptor for
     * @return the property descriptor for the specified property,
     * or {@code null} if not found
     * @throws BeansException in case of introspection failure
     */
    protected PropertyHandler getPropertyHandler(String propertyName) throws BeansException {
        Assert.notNull(propertyName, "Property name must not be null");
        AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
        return nestedPa.getLocalPropertyHandler(getFinalPath(nestedPa, propertyName));
    }

    /**
     * 抽象方法有子类实现
     */
    protected abstract PropertyHandler getLocalPropertyHandler(String propertyName);

    /**
     * 抽象方法有子类实现
     * Create a new nested property accessor instance.
     * Can be overridden in subclasses to create a PropertyAccessor subclass.
     *
     * @param object     object wrapped by this PropertyAccessor
     * @param nestedPath the nested path of the object
     * @return the nested PropertyAccessor instance
     */
    protected abstract AbstractNestablePropertyAccessor newNestedPropertyAccessor(Object object, String nestedPath);

    /**
     * 抽象方法有子类实现
     */
    protected abstract NotWritablePropertyException createNotWritablePropertyException(String propertyName);

    private Object growArrayIfNecessary(Object array, int index, String name) {
        if (!isAutoGrowNestedPaths()) {
            return array;
        }
        int length = Array.getLength(array);
        if (index >= length && index < this.autoGrowCollectionLimit) {
            Class<?> componentType = array.getClass().getComponentType();
            Object newArray = Array.newInstance(componentType, index + 1);
            System.arraycopy(array, 0, newArray, 0, length);
            for (int i = length; i < Array.getLength(newArray); i++) {
                Array.set(newArray, i, newValue(componentType, null, name));
            }
            setPropertyValue(name, newArray);
            return getPropertyValue(name);
        } else {
            return array;
        }
    }


    private void growCollectionIfNecessary(Collection<Object> collection, int index, String name,
                                           PropertyHandler ph, int nestingLevel) {
        if (!isAutoGrowNestedPaths()) {
            return;
        }
        int size = collection.size();
        if (index >= size && index < this.autoGrowCollectionLimit) {
            Class<?> elementType = ph.getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric();
            if (elementType != null) {
                for (int i = collection.size(); i < index + 1; i++) {
                    collection.add(newValue(elementType, null, name));
                }
            }
        }
    }

    /**
     * Get the last component of the path. Also works if not nested.
     *
     * @param pa         property accessor to work on
     * @param nestedPath property path we know is nested
     * @return last component of the path (the property on the target bean)
     */
    protected String getFinalPath(AbstractNestablePropertyAccessor pa, String nestedPath) {
        if (pa == this) {
            return nestedPath;
        }
        return nestedPath.substring(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex(nestedPath) + 1);
    }

    /**
     * Recursively navigate to return a property accessor for the nested property path.
     *
     * @param propertyPath property property path, which may be nested
     * @return a property accessor for the target bean
     */
    @SuppressWarnings("unchecked")  // avoid nested generic
    protected AbstractNestablePropertyAccessor getPropertyAccessorForPropertyPath(String propertyPath) {
        int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
        // Handle nested properties recursively.
        if (pos > -1) {
            String nestedProperty = propertyPath.substring(0, pos);
            String nestedPath = propertyPath.substring(pos + 1);
            AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(nestedProperty);
            return nestedPa.getPropertyAccessorForPropertyPath(nestedPath);
        } else {
            return this;
        }
    }

    /**
     * Retrieve a Property accessor for the given nested property.
     * Create a new one if not found in the cache.
     * <p>Note: Caching nested PropertyAccessors is necessary now,
     * to keep registered custom editors for nested properties.
     *
     * @param nestedProperty property to create the PropertyAccessor for
     * @return the PropertyAccessor instance, either cached or newly created
     */
    private AbstractNestablePropertyAccessor getNestedPropertyAccessor(String nestedProperty) {
        if (this.nestedPropertyAccessors == null) {
            this.nestedPropertyAccessors = new HashMap<String, AbstractNestablePropertyAccessor>();
        }
        // Get value of bean property.
        PropertyTokenHolder tokens = getPropertyNameTokens(nestedProperty);
        String canonicalName = tokens.canonicalName;
        Object value = getPropertyValue(tokens);
        if (value == null || OptionalUnwrapper.isEmpty(value)) {
            if (isAutoGrowNestedPaths()) {
                value = setDefaultValue(tokens);
            } else {
                throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + canonicalName);
            }
        }

        // Lookup cached sub-PropertyAccessor, create new one if not found.
        AbstractNestablePropertyAccessor nestedPa = this.nestedPropertyAccessors.get(canonicalName);
        if (nestedPa == null || nestedPa.getWrappedInstance() != value) {
            if (logger.isTraceEnabled()) {
                logger.trace("Creating new nested " + getClass().getSimpleName() + " for property '" + canonicalName + "'");
            }
            nestedPa = newNestedPropertyAccessor(value, this.nestedPath + canonicalName + NESTED_PROPERTY_SEPARATOR);
            // Inherit all type-specific PropertyEditors.
            copyDefaultEditorsTo(nestedPa);
            copyCustomEditorsTo(nestedPa, canonicalName);
            this.nestedPropertyAccessors.put(canonicalName, nestedPa);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Using cached nested property accessor for property '" + canonicalName + "'");
            }
        }
        return nestedPa;
    }

    private Object setDefaultValue(String propertyName) {
        PropertyTokenHolder tokens = new PropertyTokenHolder();
        tokens.actualName = propertyName;
        tokens.canonicalName = propertyName;
        return setDefaultValue(tokens);
    }

    private Object setDefaultValue(PropertyTokenHolder tokens) {
        PropertyValue pv = createDefaultPropertyValue(tokens);
        setPropertyValue(tokens, pv);
        return getPropertyValue(tokens);
    }

    private PropertyValue createDefaultPropertyValue(PropertyTokenHolder tokens) {
        TypeDescriptor desc = getPropertyTypeDescriptor(tokens.canonicalName);
        Class<?> type = desc.getType();
        if (type == null) {
            throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
                    "Could not determine property type for auto-growing a default value");
        }
        Object defaultValue = newValue(type, desc, tokens.canonicalName);
        return new PropertyValue(tokens.canonicalName, defaultValue);
    }

    private Object newValue(Class<?> type, TypeDescriptor desc, String name) {
        try {
            if (type.isArray()) {
                Class<?> componentType = type.getComponentType();
                if (componentType.isArray()) {
                    Object array = Array.newInstance(componentType, 1);
                    Array.set(array, 0, Array.newInstance(componentType.getComponentType(), 0));
                    return array;
                } else {
                    return Array.newInstance(componentType, 0);
                }
            } else if (Collection.class.isAssignableFrom(type)) {
                TypeDescriptor elementDesc = (desc != null ? desc.getElementTypeDescriptor() : null);
                return CollectionFactory.createCollection(type, (elementDesc != null ? elementDesc.getType() : null), 16);
            } else if (Map.class.isAssignableFrom(type)) {
                TypeDescriptor keyDesc = (desc != null ? desc.getMapKeyTypeDescriptor() : null);
                return CollectionFactory.createMap(type, (keyDesc != null ? keyDesc.getType() : null), 16);
            } else {
                return BeanUtils.instantiate(type);
            }
        } catch (Exception ex) {
            // Should we throw another exception type that preserves context instead?
            throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + name,
                    "Could not instantiate property type [" + type.getName() + "] to auto-grow nested property path: " + ex);
        }
    }

    /**
     * Parse the given property name into the corresponding property name tokens.
     *
     * @param propertyName the property name to parse
     * @return representation of the parsed property tokens
     */
    private PropertyTokenHolder getPropertyNameTokens(String propertyName) {
        PropertyTokenHolder tokens = new PropertyTokenHolder();
        String actualName = null;
        List<String> keys = new ArrayList<String>(2);
        int searchIndex = 0;
        while (searchIndex != -1) {
            int keyStart = propertyName.indexOf(PROPERTY_KEY_PREFIX, searchIndex);
            searchIndex = -1;
            if (keyStart != -1) {
                int keyEnd = propertyName.indexOf(PROPERTY_KEY_SUFFIX, keyStart + PROPERTY_KEY_PREFIX.length());
                if (keyEnd != -1) {
                    if (actualName == null) {
                        actualName = propertyName.substring(0, keyStart);
                    }
                    String key = propertyName.substring(keyStart + PROPERTY_KEY_PREFIX.length(), keyEnd);
                    if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\""))) {
                        key = key.substring(1, key.length() - 1);
                    }
                    keys.add(key);
                    searchIndex = keyEnd + PROPERTY_KEY_SUFFIX.length();
                }
            }
        }
        tokens.actualName = (actualName != null ? actualName : propertyName);
        tokens.canonicalName = tokens.actualName;
        if (!keys.isEmpty()) {
            tokens.canonicalName +=
                    PROPERTY_KEY_PREFIX +
                            StringUtils.collectionToDelimitedString(keys, PROPERTY_KEY_SUFFIX + PROPERTY_KEY_PREFIX) +
                            PROPERTY_KEY_SUFFIX;
            tokens.keys = StringUtils.toStringArray(keys);
        }
        return tokens;
    }

    //=============================================内部类定义=================================================================
    //=============================================内部类定义=================================================================
    //=============================================内部类定义=================================================================
    //=============================================内部类定义=================================================================
    /**内部类定义*/
    /**
     * Handle a given property.
     */
    protected abstract static class PropertyHandler {

        private final Class<?> propertyType;

        private final boolean readable;

        private final boolean writable;

        public PropertyHandler(Class<?> propertyType, boolean readable, boolean writable) {
            this.propertyType = propertyType;
            this.readable = readable;
            this.writable = writable;
        }

        public Class<?> getPropertyType() {
            return this.propertyType;
        }

        public boolean isReadable() {
            return this.readable;
        }

        public boolean isWritable() {
            return this.writable;
        }

        public abstract TypeDescriptor toTypeDescriptor();

        public abstract ResolvableType getResolvableType();

        public Class<?> getMapKeyType(int nestingLevel) {
            return getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(0);
        }

        public Class<?> getMapValueType(int nestingLevel) {
            return getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(1);
        }

        public Class<?> getCollectionType(int nestingLevel) {
            return getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric();
        }

        public abstract TypeDescriptor nested(int level);

        public abstract Object getValue() throws Exception;

        public abstract void setValue(Object object, Object value) throws Exception;
    }


    protected static class PropertyTokenHolder {

        public String canonicalName;

        public String actualName;

        public String[] keys;
    }

    /**
     * Inner class to avoid a hard dependency on Java 8.
     */
    @UsesJava8
    private static class OptionalUnwrapper {

        public static Object unwrap(Object optionalObject) {
            Optional<?> optional = (Optional<?>) optionalObject;
            Assert.isTrue(optional.isPresent(), "Optional value must be present");
            Object result = optional.get();
            Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
            return result;
        }

        public static boolean isEmpty(Object optionalObject) {
            return !((Optional<?>) optionalObject).isPresent();
        }
    }

}
