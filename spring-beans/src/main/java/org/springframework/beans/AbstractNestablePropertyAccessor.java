package org.springframework.beans;

import org.springframework.beans.exception.ConversionNotSupportedException;
import org.springframework.beans.exception.TypeMismatchException;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

import java.beans.PropertyChangeEvent;

public abstract class AbstractNestablePropertyAccessor {
    /**
     * 被包装的对账
     */
    private Object object;

    private String nestedPath = "";

    private Object rootObject;

    TypeConverterDelegate typeConverterDelegate;

    public AbstractNestablePropertyAccessor(Object object) {
        this.object = object;
        Assert.notNull(object, "target object must not be null");
    }

    public void setWrappedInstance(Object object) {
        setWrappedInstance(object, "", null);
    }


    public void setWrappedInstance(Object object, String nestedPath, Object rootObject) {
        Assert.notNull(object, "Target object must not be null");
        this.object = object;
        this.nestedPath = (nestedPath != null ? nestedPath : "");
        this.rootObject = (!"".equals(this.nestedPath) ? rootObject : this.object);
    }

    public  Object getWrappedInstance() {
        return this.object;
    }

    public Class<?> getWrappedClass() {
        return this.object.getClass();
    }
    /**
     * Return the class of the root object at the top of the path of this accessor.
     * @see #getNestedPath
     */
    public  Class<?> getRootClass() {
        return (this.rootObject != null ? this.rootObject.getClass() : null);
    }

    /**
     * Return the nested path of the object wrapped by this accessor.
     */
    public final String getNestedPath() {
        return this.nestedPath;
    }

    protected Object convertForProperty(String propertyName, Object oldValue, Object newValue, TypeDescriptor td)
            throws TypeMismatchException {

        return convertIfNecessary(propertyName, oldValue, newValue, td.getType(), td);
    }

    private Object convertIfNecessary(String propertyName, Object oldValue, Object newValue, Class<?> requiredType,
                                      TypeDescriptor td) throws TypeMismatchException {
        try {
            return this.typeConverterDelegate.convertIfNecessary(propertyName, oldValue, newValue, requiredType, td);
        }
        catch (ConverterNotFoundException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new ConversionNotSupportedException(pce, td.getType(), ex);
        }
        catch (ConversionException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new TypeMismatchException(pce, requiredType, ex);
        }
        catch (IllegalStateException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new ConversionNotSupportedException(pce, requiredType, ex);
        }
        catch (IllegalArgumentException ex) {
            PropertyChangeEvent pce =
                    new PropertyChangeEvent(this.rootObject, this.nestedPath + propertyName, oldValue, newValue);
            throw new TypeMismatchException(pce, requiredType, ex);
        }
    }

}
