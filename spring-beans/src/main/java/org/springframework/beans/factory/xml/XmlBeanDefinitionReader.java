package org.springframework.beans.factory.xml;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {
    /**
     * Indicates that the validation should be disabled.
     */
    public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

    /**
     * Indicates that the validation mode should be detected automatically.
     */
    public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

    /**
     * Indicates that DTD validation should be used.
     */
    public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

    /**
     * Indicates that XSD validation should be used.
     */
    public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;

    private String xmlPath;
    private int validationMode = VALIDATION_AUTO;

    private boolean namespaceAware = false;

    private Class<?> documentReaderClass = DefaultBeanDefinitionDocumentReader.class;

    private ProblemReporter problemReporter = new FailFastProblemReporter();

    private ReaderEventListener eventListener = new EmptyReaderEventListener();

    private SourceExtractor sourceExtractor = new NullSourceExtractor();

    private NamespaceHandlerResolver namespaceHandlerResolver;

    private DocumentLoader documentLoader = new DefaultDocumentLoader();

    private EntityResolver entityResolver;

    private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

    private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
            new NamedThreadLocal<Set<EncodedResource>>("XML bean definition resources currently being loaded");

    private BeanDefinitionParserDelegate delegate;


    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    protected EntityResolver getEntityResolver() {
        if (this.entityResolver == null) {
            // Determine default EntityResolver to use.
            ResourceLoader resourceLoader = getResourceLoader();
            /*if (resourceLoader != null) {
                this.entityResolver = new ResourceEntityResolver(resourceLoader);
            }
            else {
                this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
            }*/
        }
        return this.entityResolver;
    }

    /**
     * Load bean definitions from the specified XML file.
     *
     * @param resource the resource descriptor for the XML file
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     */
    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(new EncodedResource(resource));
    }

    /**
     * Load bean definitions from the specified XML file.
     *
     * @param encodedResource the resource descriptor for the XML file,
     *                        allowing to specify an encoding to use for parsing the file
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     */
    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
        Assert.notNull(encodedResource, "EncodedResource must not be null");
        if (logger.isInfoEnabled()) {
            logger.info("Loading XML bean definitions from " + encodedResource.getResource());
        }

        Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
        if (currentResources == null) {
            currentResources = new HashSet<EncodedResource>(4);
            this.resourcesCurrentlyBeingLoaded.set(currentResources);
        }
        if (!currentResources.add(encodedResource)) {
            throw new BeanDefinitionStoreException(
                    "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
        }
        try {
            InputStream inputStream = encodedResource.getResource().getInputStream();
            try {
                InputSource inputSource = new InputSource(inputStream);
                if (encodedResource.getEncoding() != null) {
                    inputSource.setEncoding(encodedResource.getEncoding());
                }
                // TODO 真正加载XML的方法
                return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
            } finally {
                inputStream.close();
            }
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "IOException parsing XML document from " + encodedResource.getResource(), ex);
        } finally {
            currentResources.remove(encodedResource);
            if (currentResources.isEmpty()) {
                this.resourcesCurrentlyBeingLoaded.remove();
            }
        }
    }

    /**
     * Actually load bean definitions from the specified XML file.
     *
     * @param inputSource the SAX InputSource to read from
     * @param resource    the resource descriptor for the XML file
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     * @see #doLoadDocument
     * @see #registerBeanDefinitions
     */
    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
            throws BeanDefinitionStoreException {
        try {
            Document doc = doLoadDocument(inputSource, resource);
            // 注册BeanDefinitions
            return registerBeanDefinitions(doc, resource);
        } catch (BeanDefinitionStoreException ex) {
            throw ex;
        } catch (SAXParseException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                    "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
        } catch (SAXException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                    "XML document from " + resource + " is invalid", ex);
        } catch (ParserConfigurationException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "Parser configuration exception parsing XML from " + resource, ex);
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "IOException parsing XML document from " + resource, ex);
        } catch (Throwable ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "Unexpected exception parsing XML document from " + resource, ex);
        }
    }

    /**
     * Register the bean definitions contained in the given DOM document.
     * Called by {@code loadBeanDefinitions}.
     * <p>Creates a new instance of the parser class and invokes
     * {@code registerBeanDefinitions} on it.
     *
     * @param doc      the DOM document
     * @param resource the resource descriptor (for context information)
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of parsing errors
     * @see #loadBeanDefinitions
     * @see #setDocumentReaderClass
     * @see BeanDefinitionDocumentReader#registerBeanDefinitions
     */
    public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
        BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
        int countBefore = getRegistry().getBeanDefinitionCount();
        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
        return getRegistry().getBeanDefinitionCount() - countBefore;
    }

    protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
        return BeanDefinitionDocumentReader.class.cast(BeanUtils.instantiateClass(this.documentReaderClass));
    }

    public XmlReaderContext createReaderContext(Resource resource) {
        return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
                this.sourceExtractor, this, getNamespaceHandlerResolver());
    }

    public NamespaceHandlerResolver getNamespaceHandlerResolver() {
        if (this.namespaceHandlerResolver == null) {
            this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
        }
        return this.namespaceHandlerResolver;
    }

    protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
        return new DefaultNamespaceHandlerResolver(getResourceLoader().getClassLoader());
    }

    /**
     * Actually load the specified document using the configured DocumentLoader.
     *
     * @param inputSource the SAX InputSource to read from
     * @param resource    the resource descriptor for the XML file
     * @return the DOM Document
     * @throws Exception when thrown from the DocumentLoader
     * @see #setDocumentLoader
     * @see DocumentLoader#loadDocument
     */
    protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
        return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
                getValidationModeForResource(resource), isNamespaceAware());
    }

    protected int getValidationModeForResource(Resource resource) {
        /*int validationModeToUse = getValidationMode();
        if (validationModeToUse != VALIDATION_AUTO) {
            return validationModeToUse;
        }
        int detectedMode = detectValidationMode(resource);
        if (detectedMode != VALIDATION_AUTO) {
            return detectedMode;
        }*/
        // Hmm, we didn't get a clear indication... Let's assume XSD,
        // since apparently no DTD declaration has been found up until
        // detection stopped (before finding the document's root tag).
        return VALIDATION_XSD;
    }
    /**
     * 初始化方法，在创建ClassPathXmlApplicationContext对象时初始化容器，
     * 并解析xml配置文件，获取bean元素，在运行时动态创建对象，并为对象的属性赋值，
     * 最后把对象存放在容器中以供获取
     *
     * @param xmlPath 配置文件路径
     */
    /*public void loadBeanDefinitions(String xmlPath) {
        this.xmlPath=xmlPath;
        *//*
     * 使用dom4j技术读取xml文档
     * 首先创建SAXReader对象
     *//*
        SAXReader reader = new SAXReader();
        try {
            //获取读取xml配置文件的输入流
            InputStream is = getClass().getClassLoader().getResourceAsStream(xmlPath);
            //读取xml，该操作会返回一个Document对象
            Document document = reader.read(is);
            //获取文档的根元素
            Element root = document.getRootElement();
            // 解析命名空间
            delegate.parseCustomElement(root);
            //获取根元素下所有的bean元素，elements方法会返回元素的集合
            List<Element> beanElements = root.elements("bean");
            //遍历元素集合
            for (Element beanElement : beanElements) {

                BeanDefinition bd =parseBeanDefinitionElement(beanElement);
                bd.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME); // 根据name自动装配
                getRegistry().registerBeanDefinition(bd.getBeanName(), bd); // 注册BeanDefinition

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public BeanDefinition parseBeanDefinitionElement(Element beanElement) throws ClassNotFoundException {
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
    }*/

}
