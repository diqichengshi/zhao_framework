package org.springframework.beans;

import org.springframework.beans.exception.InvalidPropertyException;
import org.springframework.beans.exception.NotWritablePropertyException;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

    private String beanName;

    private CachedIntrospectionResults cachedIntrospectionResults;

    public BeanWrapperImpl() {
        this(true);
    }

    public BeanWrapperImpl(Object beanInstance) {
        super(beanInstance); // typeConverterDelegate赋值
    }

    private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
        super(object, nestedPath, parent);
    }

    public Object getWrappedInstance() {
        return super.getWrappedInstance();
    }

    public Class<?> getWrappedClass() {
        return super.getWrappedClass();
    }

    /**
     * Convert the given value for the specified property to the latter's type.
     * <p>This method is only intended for optimizations in a BeanFactory.
     * Use the {@code convertIfNecessary} methods for programmatic conversion.
     *
     * @param value        the value to convert
     * @param propertyName the target property
     *                     (note that nested or indexed properties are not supported here)
     * @return the new value, possibly the result of type conversion
     * @throws TypeMismatchException if type conversion failed
     */
    public Object convertForProperty(Object value, String propertyName) throws TypeMismatchException {
        CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
        PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
                    "No property '" + propertyName + "' found");
        }
        TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
        if (td == null) {
            td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
        }
        return convertForProperty(propertyName, null, value, td);
    }

    private Property property(PropertyDescriptor pd) {
        GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
        return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return getCachedIntrospectionResults().getPropertyDescriptors();
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
        BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
        String finalPath = getFinalPath(nestedBw, propertyName);
        PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
                    "No property '" + propertyName + "' found");
        }
        return pd;
    }

    /**
     * Obtain a lazily initializted CachedIntrospectionResults instance
     * for the wrapped object.
     */
    private CachedIntrospectionResults getCachedIntrospectionResults() {
        Assert.state(getWrappedInstance() != null, "BeanWrapper does not hold a bean instance");
        if (this.cachedIntrospectionResults == null) {
            this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
        }
        return this.cachedIntrospectionResults;
    }

    @Override
    protected AbstractNestablePropertyAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
        return new BeanWrapperImpl(object, nestedPath, this);
    }

    @Override
    protected PropertyHandler getLocalPropertyHandler(String propertyName) {
        PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
        if (pd != null) {
            return new BeanPropertyHandler(pd);
        }
        return null;
    }

    @Override
    protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
        PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
        throw new NotWritablePropertyException(
                getRootClass(), getNestedPath() + propertyName,
                matches.buildErrorMessage(), matches.getPossibleMatches());
    }

    //======================================内部类定义====================================================

    /**
     * 内部类定义,PropertyHandler定义在父类中
     */
    private class BeanPropertyHandler extends PropertyHandler {

        private final PropertyDescriptor pd;

        public BeanPropertyHandler(PropertyDescriptor pd) {
            super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
            this.pd = pd;
        }

        @Override
        public ResolvableType getResolvableType() {
            return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
        }

        @Override
        public TypeDescriptor toTypeDescriptor() {
            return new TypeDescriptor(property(this.pd));
        }

        @Override
        public TypeDescriptor nested(int level) {
            return TypeDescriptor.nested(property(pd), level);
        }

        @Override
        public Object getValue() throws Exception {
            final Method readMethod = this.pd.getReadMethod();
            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers()) && !readMethod.isAccessible()) {
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            readMethod.setAccessible(true);
                            return null;
                        }
                    });
                } else {
                    readMethod.setAccessible(true);
                }
            }
            return readMethod.invoke(getWrappedInstance(), (Object[]) null);
        }

        @Override
        public void setValue(final Object object, Object valueToApply) throws Exception {
            final Method writeMethod = this.pd.getWriteMethod();
            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers()) && !writeMethod.isAccessible()) {
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            writeMethod.setAccessible(true);
                            return null;
                        }
                    });
                } else {
                    writeMethod.setAccessible(true);
                }
            }
            final Object value = valueToApply;

            writeMethod.invoke(getWrappedInstance(), value);
        }
    }

}
