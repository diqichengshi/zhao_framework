package org.springframework.beans;

import org.springframework.beans.exception.InvalidPropertyException;
import org.springframework.beans.exception.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.List;

public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {
    private String beanName;


    private CachedIntrospectionResults cachedIntrospectionResults;

    public BeanWrapperImpl(Object beanInstance) {
       super(beanInstance);
    }

    public Object getWrappedInstance() {
        return super.getWrappedInstance();
    }

    public Class<?> getWrappedClass() {
        return super.getWrappedClass();
    }

    /**
     * 设置配置属性
     */
    @Override
    public void setPropertyValues(PropertyValues pvs) throws BeansException {
        Object object=getWrappedInstance();
        Class<?> cls = getWrappedInstance().getClass();
        List<PropertyValue> list = pvs.getPropertyValueList();

        for (int i = 0; i < list.size(); i++) {
            PropertyValue pv = list.get(i);
            try {
                //利用反射技术根据name属性值获得类的成员属性
                Field field = cls.getDeclaredField(pv.getName());
                //将该属性设置为可访问(防止成员属性被私有化导致访问失败)
                field.setAccessible(true);
                //获取成员属性的类型名称，若非字符串类型，则需要做相应转换
                String fieldTypeName = field.getType().getName();
                //判断该成员属性是否为int或Integer类型
                if ("int".equals(fieldTypeName) || "java.lang.Integer".equals(fieldTypeName)) {
                    //转换为int类型并为该成员属性赋值
                    int intFieldValue = Integer.parseInt(pv.getValue().toString());
                    field.set(object, intFieldValue);
                }
                //判断该成员属性是否为String,对象类型
                if ("java.lang.String".equals(fieldTypeName) ) {
                    //为该成员属性赋值
                    field.set(object, pv.getValue());
                }
                if ("java.lang.Object".equals(fieldTypeName)  ) {
                    //为该成员属性赋值
                    field.set(object, pv.getValue());
                }
                //此处省略其它类型的判断......道理相同！
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                throw new BeansException(beanName + " set " + pv.getName() + "值" + pv.getValue() + "错误");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new BeansException(beanName + " set " + pv.getName() + "值" + pv.getValue() + "错误");
            }

        }
    }

    /**
     * Convert the given value for the specified property to the latter's type.
     * <p>This method is only intended for optimizations in a BeanFactory.
     * Use the {@code convertIfNecessary} methods for programmatic conversion.
     * @param value the value to convert
     * @param propertyName the target property
     * (note that nested or indexed properties are not supported here)
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

}
