package com.zhaojuan.spring.beans;

import com.zhaojuan.spring.beans.annotation.Autowired;
import com.zhaojuan.spring.beans.annotation.Service;
import com.zhaojuan.spring.beans.factory.support.AbstractBeanFactory;
import com.zhaojuan.spring.core.util.ClassUtils;
import com.zhaojuan.spring.core.util.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationBeanFactory extends AbstractBeanFactory implements BeanFactory, BeanDefinitionRegistry {

    // 扫包范围
    private String packageName;
    private ConcurrentHashMap<String, Object> beans;

    public AnnotationBeanFactory(String packageName) throws Exception {
        this.packageName = packageName;
        //beans = new ConcurrentHashMap<String, Object>();
        // 初始化beans Service注解
        initBeans();
        // 初始化属性以及Autowired注解
        initAttributes();
    }

    private void initBeans() {
        // 1.扫包
        List<Class<?>> classes = ClassUtils.getClasses(packageName);
        // 2.判断是否有注解
        ConcurrentHashMap<String, Object> findClassExistAnnotation = findClassExistAnnotation(classes);
        if (findClassExistAnnotation == null || findClassExistAnnotation.isEmpty()) {
            throw new RuntimeException("该包下没有这个注解");
        }
    }

    private void initAttributes() throws Exception {
        for (Object obj : beans.keySet()) {
            System.out.println("key=" + obj + " value=" + beans.get(obj));
            // 依赖注入
            attriAssign(beans.get(obj));
        }
    }

    public Object getBean(String beanId) {
        if (beanId == null || StringUtils.isEmpty(beanId)) {
            throw new RuntimeException("beanId不能为空");
        }
        Object obj = beans.get(beanId);
        if (obj == null) {
            throw new RuntimeException("该包下没有BeanId为" + beanId + "的类");
        }
        return obj;
    }

    /**
     * 是否有注解
     */
    private ConcurrentHashMap<String, Object> findClassExistAnnotation(List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            Service annotation = clazz.getAnnotation(Service.class);
            if (null != annotation) {
                // beanId类名小写
                String beanId = annotation.value();
                if (StringUtils.isEmpty(beanId)) {
                    beanId = StringUtils.toLowerCaseFirstOne(clazz.getSimpleName());// 获取当前类名
                }
                Object newInstance = ClassUtils.newInstance(clazz);
                beans.put(beanId, newInstance);
            }
        }
        return beans;
    }

    /**
     * 依赖注入
     */
    private void attriAssign(Object object) throws Exception {
        // 使用反射机制获取当前类的所以属性
        Field[] declaredFileds = object.getClass().getDeclaredFields();
        // 判断当前类是否存在注解
        for (Field field : declaredFileds) {
            Autowired annotation = field.getAnnotation(Autowired.class);
            if (null !=annotation){
                String name = field.getName();
                Object newBean = getBean(name);
                if (null != object) {
                    field.setAccessible(true);
                    field.set(object, newBean);
                }
            }
        }
    }


    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition hkBeanDefinition) throws Exception {

    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return null;
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return false;
    }

    @Override
    protected Object createBean(String beanName, BeanDefinition mbd, Object[] args) throws BeanCreationException {
        return null;
    }
}