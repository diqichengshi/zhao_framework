package com.zhaojuan.spring.beans;

import org.dom4j.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanFactory extends AbstractBeanFactory implements BeanFactory, BeanDefinitionRegistry {

    private Map<String, Object> beanMap = new ConcurrentHashMap<String, Object>();
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();

    /*
     * 注册bean定义，需要给定唯一bean的名称和bean的定义,放到bean定义集合中
     */
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws Exception {
        Objects.requireNonNull(beanName, "beanName不能为空");
        Objects.requireNonNull(beanDefinition, "beanDefinition不能为空");
        if (beanDefinitionMap.containsKey(beanName)) {
            throw new Exception("已存在【" + beanName + "】的bean定义" + getBeanDefinition(beanName));
        }
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    /*
     * 获得bean的门面方法
     * 采用构造器来创建对象
     */
    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name);
    }

    /**
     * getBean的具体逻辑
     * <p>
     * 事实上，在spring的bean定义中，还可以静态工厂方法和成员工厂方法来创建实例，但在开发中这2种用的较少，所以此处只使用构造器来创建bean
     */
    private Object doGetBean(String beanName) throws BeansException {
        Objects.requireNonNull(beanName, "beanName不能为空");
        Object instance = beanMap.get(beanName);
        //如果bean已存在，则直接返回
        if (instance != null) {
            return instance;
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        Objects.requireNonNull(beanDefinition, "beanDefinition不能为空");
        Class<?> class1 = beanDefinition.getBeanClass();
        Objects.requireNonNull(class1, "bean定义中class类型不能为空");

        //到了核心方法了,创建一个真正的Bean跟进分析
        instance = doCreateBean(beanName, beanDefinition);

        //将单例bean放到map中，下次可直接拿到
        if (beanDefinition.isSingleton()) {
            beanMap.put(beanName, instance);
        }
        return instance;
    }

    protected Object doCreateBean(final String beanName, final BeanDefinition bd) {
        // Instantiate the bean.
        BeanWrapper instanceWrapper = null;
        if (instanceWrapper == null) {
            //这个是主要方法一，生成wrapper下面分析
            instanceWrapper = createBeanInstance(beanName, bd);
        }
        //获取bean和class对象
        final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
        Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);
        // Initialize the bean instance.
        Object exposedObject = bean;
        try {
            //属性填充，重点方法二
            populateBean(beanName, bd, instanceWrapper);
            if (exposedObject != null) {
                //实现InitializingBean接口的方法回调，重点方法三
                exposedObject = initializeBean(beanName, exposedObject, bd);
            }
        } catch (Throwable ex) {
            if (ex instanceof BeansException) {
                throw (BeansException) ex;
            } else {
                throw new BeansException(beanName + "Initialization of bean failed", ex);
            }
        }
        return exposedObject;
    }

    private BeanWrapper createBeanInstance(String beanName, BeanDefinition bd) {
        return instantiateBean(beanName, bd);
    }

    /**
     * 属性填充
     */
    public void populateBean(String beanName, BeanDefinition bd, BeanWrapper bw) {
        PropertyValues pvs = bd.getPropertyValues();
        applyPropertyValues(beanName, bd, bw, pvs);
    }

    /**
     * 对象的属性赋值
     */
    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        List<PropertyValue> original;
        if (pvs instanceof MutablePropertyValues) {
            original = pvs.getPropertyValueList();
        } else {
            original = Arrays.asList(pvs.getPropertyValues());
        }
        // 创建深度副本,解析值的任何引用
        List<PropertyValue> deepCopy = new ArrayList<PropertyValue>(original.size());
        for (PropertyValue pv : original) {
            deepCopy.add(pv);
        }

        // 设置我们的深度副本
        try {
            bw.setPropertyValues(new MutablePropertyValues(deepCopy));
        } catch (Exception ex) {
            throw new BeansException(beanName + "Error setting property values" + ex);
        }

    }

    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitionMap.get(beanName);
    }

    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

}

