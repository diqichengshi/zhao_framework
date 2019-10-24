package org.springframework.beans;

import org.springframework.beans.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XmlBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry {

    /**
     * 存储beanElement对象容器
     */
    private Map<String, Element> beanElementMap;
    /**
     * 存储bean的scope属性容器
     */
    private Map<String, String> beanScopeMap;

    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();

    /**
     * 有参的构造方法，在创建此类实例时需要指定xml文件路径
     */
    public XmlBeanFactory(String xmlPath) {
        // 初始化容器
        beanElementMap = new ConcurrentHashMap<String, Element>();
        beanScopeMap = new ConcurrentHashMap<String, String>();
        // 解析xml文件
        loadBeanDefinitions(xmlPath);
    }


    /**
     * 初始化方法，在创建ClassPathXmlApplicationContext对象时初始化容器，
     * 并解析xml配置文件，获取bean元素，在运行时动态创建对象，并为对象的属性赋值，
     * 最后把对象存放在容器中以供获取
     *
     * @param xmlPath 配置文件路径
     */
    private void loadBeanDefinitions(String xmlPath) {
        /*
         * 使用dom4j技术读取xml文档
         * 首先创建SAXReader对象
         */
        SAXReader reader = new SAXReader();
        try {
            //获取读取xml配置文件的输入流
            InputStream is = getClass().getClassLoader().getResourceAsStream(xmlPath);
            //读取xml，该操作会返回一个Document对象
            Document document = reader.read(is);
            //获取文档的根元素
            Element rootElement = document.getRootElement();
            //获取根元素下所有的bean元素，elements方法会返回元素的集合
            List<Element> beanElements = rootElement.elements("bean");
            //遍历元素集合
            for (Element beanElement : beanElements) {

                BeanDefinition bd =parseBeanDefinitionElement(beanElement);
                bd.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME); // 根据name自动装配
                registerBeanDefinition(bd.getBeanName(), bd); // 注册BeanDefinition

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    public BeanDefinition parseBeanDefinitionElement(Element beanElement ) throws ClassNotFoundException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        //获取bean的id值，该值用于作为key存储于Map集合中
        String beanId = beanElement.attributeValue("id");
        bd.setBeanName(beanId);
        //获取bean的scope值
        String beanScope = beanElement.attributeValue("scope");
        //如果beanScope不等于null，将bean的scope值存入map中方便后续使用
        if (beanScope != null) {
            bd.setScope(beanScope);
        }
        //获取bean的class路径
        String beanClassPath = beanElement.attributeValue("class");
        //利用反射技术根据获得的beanClass路径得到类定义对象
        Class<?> cls = Class.forName(beanClassPath);
        //如果反射获取的类定义对象不为null，则放入工厂中方便创建其实例对象
        if (cls != null) {
            bd.setBeanClass(cls);
        }
        List<Element> propElements = beanElement.elements("property");
        //如果property元素集合为null，调用putInMap方法将对象放进Map中
        if (propElements != null) {
            List<PropertyValue> pvs = new ArrayList<>();
            List<String> refList = new ArrayList<>();
            //遍历property元素集合
            for (Element propElement : propElements) {
                //获取每个元素的name属性值和value属性值
                String fieldName = propElement.attributeValue("name");
                String fieldValue = propElement.attributeValue("value");
                if (StringUtils.isNotEmpty(fieldValue)){
                    PropertyValue pv = new PropertyValue(fieldName, fieldValue);
                    pvs.add(pv);
                }else {
                    String ref = propElement.attributeValue("ref");
                    refList.add(ref); // 需要依赖注入的属性
                    PropertyValue pv = new PropertyValue(fieldName, ref);
                    pvs.add(pv);
                }
            }
            MutablePropertyValues mpvs = new MutablePropertyValues(pvs);

            bd.setPropertyValues(mpvs);
            bd.setDependsOn(refList);
        }
        return bd;
    }


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


    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitionMap.get(beanName);
    }

    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

}