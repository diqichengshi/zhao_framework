package com.zhaojuan.spring.beans;

import com.zhaojuan.spring.core.util.Assert;

import java.lang.reflect.Field;
import java.util.List;

public class BeanWrapperImpl implements BeanWrapper {
    private String beanName;
    /**
     * 被包装的对账
     */
    private Object object;

    public BeanWrapperImpl(Object beanInstance) {
        Assert.notNull(object, "target object must not be null");
        this.object = object;
    }

    @Override
    public Object getWrappedInstance() {
        return object;
    }

    @Override
    public Class<?> getWrappedClass() {
        return object.getClass();
    }

    /**
     * 设置配置属性
     */
    @Override
    public void setPropertyValues(PropertyValues pvs) throws BeansException {
        Class<?> cls=object.getClass();
        List<PropertyValue> list= pvs.getPropertyValueList();

        for (int i = 0; i < list.size(); i++) {
            PropertyValue pv=list.get(i);
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
                //判断该成员属性是否为String类型
                if ("java.lang.String".equals(fieldTypeName)) {
                    //为该成员属性赋值
                    field.set(object, pv.getValue());
                }
                //此处省略其它类型的判断......道理相同！
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                throw new BeansException(beanName + " set " + pv.getName() + "值" + pv.getValue() + "错误");
            }catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new BeansException(beanName + " set " + pv.getName() + "值" + pv.getValue() + "错误");
            }

        }
    }

}
