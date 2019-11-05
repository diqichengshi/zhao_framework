package org.springframework.beans;

import org.springframework.beans.exception.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements PropertyAccessor {

    private boolean extractOldValueForEditor = false;

    private boolean autoGrowNestedPaths = false;

    public boolean isExtractOldValueForEditor() {
        return this.extractOldValueForEditor;
    }

    public boolean isAutoGrowNestedPaths() {
        return this.autoGrowNestedPaths;
    }

    public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
        this.extractOldValueForEditor = extractOldValueForEditor;
    }

    public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    @Override
    public void setPropertyValues(PropertyValues pvs) throws BeansException {
        setPropertyValues(pvs, false, false);
    }

    /**
     * 设置配置属性
     */
    @Override
    public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid) throws BeansException {
        List<PropertyAccessException> propertyAccessExceptions = null;
        List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ?
                ((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));
        for (PropertyValue pv : propertyValues) {
            try {
                // This method may throw any BeansException, which won't be caught
                // here, if there is a critical failure such as no matching field.
                // We can attempt to deal only with less serious exceptions.
                // TODO 设置属性值 此处调用子类方法
                setPropertyValue(pv);
            } catch (NotWritablePropertyException ex) {
                throw ex;
                // Otherwise, just ignore it and continue...
            } catch (NullValueInNestedPathException ex) {
                throw ex;
                // Otherwise, just ignore it and continue...
            } catch (PropertyAccessException ex) {
                if (propertyAccessExceptions == null) {
                    propertyAccessExceptions = new LinkedList<PropertyAccessException>();
                }
                propertyAccessExceptions.add(ex);
            }
        }

        // If we encountered individual exceptions, throw the composite exception.
        if (propertyAccessExceptions != null) {
            PropertyAccessException[] paeArray =
                    propertyAccessExceptions.toArray(new PropertyAccessException[propertyAccessExceptions.size()]);
            throw new PropertyBatchUpdateException(paeArray);
        }

    }
    /**
     * Actually set a property value.
     *
     * @param propertyName name of the property to set value of
     * @param value        the new value
     * @throws InvalidPropertyException if there is no such property or
     *                                  if the property isn't writable
     * @throws PropertyAccessException  if the property was valid but the
     *                                  accessor method failed or a type mismatch occured
     */
   /* @Override
    public abstract void setPropertyValue(String propertyName, Object value) throws BeansException;*/

}
