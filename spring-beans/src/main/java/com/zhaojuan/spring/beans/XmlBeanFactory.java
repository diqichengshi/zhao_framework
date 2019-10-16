package com.zhaojuan.spring.beans;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XmlBeanFactory extends DefaultBeanFactory {

    /**
     * 存储beanElement对象容器
     */
    private Map<String, Element> beanElementMap;
    /**
     * 存储bean的scope属性容器
     */
    private Map<String, String> beanScopeMap;

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
            for (Element beanEle : beanElements) {
                //获取bean的id值，该值用于作为key存储于Map集合中
                String beanId = beanEle.attributeValue("id");
                //将beanElement对象存入map中，为对象设置属性值时使用
                beanElementMap.put(beanId, beanEle);
                //获取bean的scope值
                String beanScope = beanEle.attributeValue("scope");
                //如果beanScope不等于null，将bean的scope值存入map中方便后续使用
                if (beanScope != null) {
                    beanScopeMap.put(beanId, beanScope);
                }
                //获取bean的class路径
                String beanClassPath = beanEle.attributeValue("class");
                //利用反射技术根据获得的beanClass路径得到类定义对象
                Class<?> cls = Class.forName(beanClassPath);
                //如果反射获取的类定义对象不为null，则放入工厂中方便创建其实例对象
                if (cls != null) {
                    GenericBeanDefinition bd = new GenericBeanDefinition();
                    bd.setBeanClass(cls);
                    bd.setScope(beanScope);
                    // bd.setInitMethodName("init");
                    super.registerBeanDefinition(beanId, bd);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Object getBean(String name) throws Exception {
        Object obj = super.getBean(name);
        //根据id获取该bean对象的element元素对象
        Element beanElement = beanElementMap.get(name);
        String beanScope = beanScopeMap.get(name);
        // 进行属性赋值
        setFieldValues(beanElement, beanScope, obj.getClass(), obj);
        return obj;
    }

    /**
     * 该方法用于为对象设置成员属性值
     *
     * @param beanEle   bean所对应的element对象
     * @param beanId    bean元素的id属性
     * @param beanScope bean元素的scope属性
     * @param cls       类对象
     * @param obj       要为其成员属性赋值的实例对象
     */
    private void setFieldValues(Element beanEle, String beanScope, Class<?> cls, Object obj) {
        try {
            //获取每个bean元素下的所有property元素，该元素用于给属性赋值
            List<Element> propEles = beanEle.elements("property");
            //如果property元素集合为null，调用putInMap方法将对象放进Map中
            if (propEles == null) {
                return;
            }
            //遍历property元素集合
            for (Element propEle : propEles) {
                //获取每个元素的name属性值和value属性值
                String fieldName = propEle.attributeValue("name");
                String fieldValue = propEle.attributeValue("value");
                //利用反射技术根据name属性值获得类的成员属性
                Field field = cls.getDeclaredField(fieldName);
                //将该属性设置为可访问(防止成员属性被私有化导致访问失败)
                field.setAccessible(true);
                //获取成员属性的类型名称，若非字符串类型，则需要做相应转换
                String fieldTypeName = field.getType().getName();
                //判断该成员属性是否为int或Integer类型
                if ("int".equals(fieldTypeName) || "java.lang.Integer".equals(fieldTypeName)) {
                    //转换为int类型并为该成员属性赋值
                    int intFieldValue = Integer.parseInt(fieldValue);
                    field.set(obj, intFieldValue);
                }
                //判断该成员属性是否为String类型
                if ("java.lang.String".equals(fieldTypeName)) {
                    //为该成员属性赋值
                    field.set(obj, fieldValue);
                }
                //此处省略其它类型的判断......道理相同！
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}