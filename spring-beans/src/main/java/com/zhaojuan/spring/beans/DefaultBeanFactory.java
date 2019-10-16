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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanFactory implements BeanFactory, BeanDefinitionRegistry {

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
    public Object getBean(String name) throws Exception {
        return doGetBean(name);
    }

    /**
     * getBean的具体逻辑
     * <p>
     * 事实上，在spring的bean定义中，还可以静态工厂方法和成员工厂方法来创建实例，但在开发中这2种用的较少，所以此处只使用构造器来创建bean
     */
    private Object doGetBean(String beanName) throws Exception {
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
        instance = class1.newInstance();

        //实例已创建好，通过反射执行bean的init方法
        String initMethodName = beanDefinition.getInitMethodName();
        if (null != initMethodName) {
            Method method = class1.getMethod(initMethodName, null);
            method.invoke(instance, null);
        }

        //将单例bean放到map中，下次可直接拿到
        if (beanDefinition.isSingleton()) {
            beanMap.put(beanName, instance);
        }
        return instance;
    }

    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitionMap.get(beanName);
    }

    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

}

