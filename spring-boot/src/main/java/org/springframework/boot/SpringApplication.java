/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import java.lang.reflect.Constructor;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.diagnostics.FailureAnalyzers;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Classes that can be used to bootstrap and launch a Spring application from a Java main
 * method. By default class will perform the following steps to bootstrap your
 * application:
 *
 * <ul>
 * <li>Create an appropriate {@link ApplicationContext} instance (depending on your
 * classpath)</li>
 * <li>Register a {@link CommandLinePropertySource} to expose command line arguments as
 * Spring properties</li>
 * <li>Refresh the application context, loading all singleton beans</li>
 * <li>Trigger any {@link CommandLineRunner} beans</li>
 * </ul>
 *
 * In most circumstances the static {@link #run(Object, String[])} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAutoConfiguration
 * public class MyApplication  {
 *
 *   // ... Bean definitions
 *
 *   public static void main(String[] args) throws Exception {
 *     SpringApplication.run(MyApplication.class, args);
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more advanced configuration a {@link SpringApplication} instance can be created and
 * customized before being run:
 *
 * <pre class="code">
 * public static void main(String[] args) throws Exception {
 *   SpringApplication app = new SpringApplication(MyApplication.class);
 *   // ... customize app settings here
 *   app.run(args)
 * }
 * </pre>
 *
 * {@link SpringApplication}s can read beans from a variety of different sources. It is
 * generally recommended that a single {@code @Configuration} class is used to bootstrap
 * your application, however, any of the following sources can also be used:
 *
 * <ul>
 * <li>{@link Class} - A Java class to be loaded by {@link AnnotatedBeanDefinitionReader}
 * </li>
 * <li>{@link Resource} - An XML resource to be loaded by {@link XmlBeanDefinitionReader},
 * or a groovy script to be loaded by {@link GroovyBeanDefinitionReader}</li>
 * <li>{@link Package} - A Java package to be scanned by
 * {@link ClassPathBeanDefinitionScanner}</li>
 * <li>{@link CharSequence} - A class name, resource handle or package name to loaded as
 * appropriate. If the {@link CharSequence} cannot be resolved to class and does not
 * resolve to a {@link Resource} that exists it will be considered a {@link Package}.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 * @author Craig Burke
 * @author Michael Simons
 * @author Ethan Rubinson
 * @since 1.0.0
 * @see #run(Object, String[])
 * @see #run(Object[], String[])
 * @see #SpringApplication(Object...)
 */
public class SpringApplication {

    /**
     * The class name of application context that will be used by default for non-web
     * environments.
     * 在非WEB环境下提供的默认ApplicationContext类型
     */
    public static final String DEFAULT_CONTEXT_CLASS = "org.springframework.context."
            + "annotation.AnnotationConfigApplicationContext";

    /**
     * The class name of application context that will be used by default for web
     * environments.
     * 在WEB环境下提供的默认ApplicationContext类型
     */
    public static final String DEFAULT_WEB_CONTEXT_CLASS = "org.springframework."
            + "boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext";

    private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
            "org.springframework.web.context.ConfigurableWebApplicationContext" };

    private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

    private static final Log logger = LogFactory.getLog(SpringApplication.class);
    /**
     * SpringApplication的入口源
     */
    private final Set<Object> sources = new LinkedHashSet<Object>();
    /**
     * 提供main方法的类
     */
    private Class<?> mainApplicationClass;

    private boolean logStartupInfo = true;

    private boolean addCommandLineProperties = true;

    /**
     * 通过加载外部资源启动Spring
     */
    private ResourceLoader resourceLoader;
    /**
     * Bean名称生成器
     */
    private BeanNameGenerator beanNameGenerator;
    /**
     * Spring 环境
     */
    private ConfigurableEnvironment environment;

    /**
     * 运行的ApplicationContext类型
     */
    private Class<? extends ConfigurableApplicationContext> applicationContextClass;

    /**
     * 标记是否是WEB环境
     */
    private boolean webEnvironment;

    private boolean headless = true;

    private boolean registerShutdownHook = true;
    /**
     * Spring上下文初始化器
     */
    private List<ApplicationContextInitializer<?>> initializers;
    /**
     * Spring监听器
     */
    private List<ApplicationListener<?>> listeners;
    /**
     * 设置默认属性
     */
    private Map<String, Object> defaultProperties;
    /**
     * 添加profile，可以支持多个profile
     */
    private Set<String> additionalProfiles = new HashSet<String>();

    /**
     * Create a new {@link SpringApplication} instance. The application context will load
     * beans from the specified sources (see {@link SpringApplication class-level}
     * documentation for details. The instance can be customized before calling
     * {@link #run(String...)}.
     * @param sources the bean sources
     * @see #run(Object, String[])
     * @see #SpringApplication(ResourceLoader, Object...)
     */
    public SpringApplication(Object... sources) {
        initialize(sources);
    }

    /**
     * Create a new {@link SpringApplication} instance. The application context will load
     * beans from the specified sources (see {@link SpringApplication class-level}
     * documentation for details. The instance can be customized before calling
     * {@link #run(String...)}.
     * @param resourceLoader the resource loader to use
     * @param sources the bean sources
     * @see #run(Object, String[])
     * @see #SpringApplication(ResourceLoader, Object...)
     */
    public SpringApplication(ResourceLoader resourceLoader, Object... sources) {
        this.resourceLoader = resourceLoader;
        // TODO SpringApplication初始化
        initialize(sources);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initialize(Object[] sources) {
        // 设置启动源
        if (sources != null && sources.length > 0) {
            this.sources.addAll(Arrays.asList(sources));
        }
        // 判断是否是WEB环境
        this.webEnvironment = deduceWebEnvironment();
        // TODO 设置ApplicationContext初始化器
        // DelegatingApplicationContextInitializer
        setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
        // TODO 设置Application监听器
        setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
        // 主方法运行类
        this.mainApplicationClass = deduceMainApplicationClass();
    }

    private boolean deduceWebEnvironment() {
        for (String className : WEB_ENVIRONMENT_CLASSES) {
            if (!ClassUtils.isPresent(className, null)) {
                return false;
            }
        }
        return true;
    }

    private Class<?> deduceMainApplicationClass() {
        try {
            // TODO 根据main方法找到应用启动类
            // 利用RuntimeException().getStackTrace()推断出当前执行流中的某个类
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if ("main".equals(stackTraceElement.getMethodName())) {
                    return Class.forName(stackTraceElement.getClassName());
                }
            }
        }
        catch (ClassNotFoundException ex) {
            // Swallow and continue
        }
        return null;
    }

    /**
     * Run the Spring application, creating and refreshing a new
     * {@link ApplicationContext}.
     * @param args the application arguments (usually passed from a Java main method)
     * @return a running {@link ApplicationContext}
     */
    public ConfigurableApplicationContext run(String... args) {
        //  TODO spring-boot源码解析 应用启动
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ConfigurableApplicationContext context = null;
        FailureAnalyzers analyzers = null;
        configureHeadlessProperty();
        // 初始化SpringApplicationRunListener
        SpringApplicationRunListeners listeners = getRunListeners(args);
        // 开启监听器
        listeners.starting();
        try {
            // args封装到ApplicationArguments应用参数
            ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
            // 将监听器listeners和应用参数applicationArguments封装到配置环境ConfigurableEnvironment中去
            ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
            // 创建ApplicationContext
            context = createApplicationContext();
            analyzers = new FailureAnalyzers(context);
            // 将environment配置环境,listeners监听器列表,applicationArguments应用参数,printedBanner封装到context中
            prepareContext(context, environment, listeners, applicationArguments);
            // 刷新spring上下文
            refreshContext(context);
            afterRefresh(context, applicationArguments); // callRunners
            // 执行监听器spring加载完成事件(发布applicationEvent事件)
            listeners.finished(context, null);
            stopWatch.stop();
            if (this.logStartupInfo) {
                new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
            }
            return context;
        }
        catch (Throwable ex) {
            handleRunFailure(context, listeners, analyzers, ex);
            throw new IllegalStateException(ex);
        }
    }

    private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
                                                       ApplicationArguments applicationArguments) {
        // Create and configure the environment
        // 创建Spring环境
        ConfigurableEnvironment environment = getOrCreateEnvironment();
        // TODO 配置Spring环境(配置属性源,配置profiles)
        configureEnvironment(environment, applicationArguments.getSourceArgs());
        // TODO 通知监听器,主要是这步,此处底层调用listener.onApplicationEvent()事件
        // TODO ConfigFileApplicationListener就是在此处开始解析配置文件
        listeners.environmentPrepared(environment);
        // 如果不是WEB环境,转化为标准的Spring环境
        if (!this.webEnvironment) {
            environment = new EnvironmentConverter(getClassLoader())
                    .convertToStandardEnvironmentIfNecessary(environment);
        }
        return environment;
    }

    /**
     * 将Spring环境设置到上下文中去
     * 注册Bean名称生成器和设置资源加载器.Bean名称生成器可以通过setter方法设置,资源加载器可以通过构造函数和setter方法设置.
     * 调用上下文初始化器
     * 将参数注册到Spring容器
     * 加载Spring资源,SpringBoot入口源
     */
    private void prepareContext(ConfigurableApplicationContext context, ConfigurableEnvironment environment,
                                SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments) {
        // 将环境设置到上下文
        context.setEnvironment(environment);
        // 后置处理上下文,主要注册Bean名称生成器和设置资源加载器
        postProcessApplicationContext(context);
        // 调用上下文初始化器
        applyInitializers(context);
        // 通知监听器上下文已经准备完毕
        listeners.contextPrepared(context);
        if (this.logStartupInfo) {
            logStartupInfo(context.getParent() == null);
            logStartupProfileInfo(context);
        }

        // Add boot specific singleton beans
        // 将参数注册到Spring容器
        context.getBeanFactory().registerSingleton("springApplicationArguments", applicationArguments);

        // Load the sources
        // 加载Spring资源,也就是SpringBoot入口源
        Set<Object> sources = getSources();
        Assert.notEmpty(sources, "Sources must not be empty");
        // TODO 加载bean定义
        load(context, sources.toArray(new Object[sources.size()]));
        // 通知监听器上下文被加载完毕
        listeners.contextLoaded(context);
    }

    private void refreshContext(ConfigurableApplicationContext context) {
        refresh(context);
        if (this.registerShutdownHook) {
            try {
                context.registerShutdownHook();
            }
            catch (AccessControlException ex) {
                // Not allowed in some environments.
            }
        }
    }

    private void configureHeadlessProperty() {
        System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS,
                System.getProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
    }

    private SpringApplicationRunListeners getRunListeners(String[] args) {
        // 初始化SpringApplicationRunListener
        // # Run Listeners
        // org.springframework.boot.SpringApplicationRunListener=\
        // org.springframework.boot.context.event.EventPublishingRunListener
        Class<?>[] types = new Class<?>[] { SpringApplication.class, String[].class };
        return new SpringApplicationRunListeners(logger,
                getSpringFactoriesInstances(SpringApplicationRunListener.class, types, this, args));
    }

    private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type) {
        return getSpringFactoriesInstances(type, new Class<?>[] {});
    }

    private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes,
                                                                    Object... args) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // Use names and ensure unique to protect against duplicates
        // 从META-INF/spring.factories路径加载
        Set<String> names = new LinkedHashSet<String>(SpringFactoriesLoader.loadFactoryNames(type, classLoader));
        // 创建META-INF/spring.factories配置的对象
        List<T> instances = createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names);
        AnnotationAwareOrderComparator.sort(instances);
        return instances;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> createSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes,
                                                       ClassLoader classLoader, Object[] args, Set<String> names) {
        //创建实例list,用来装创建好的实例
        List<T> instances = new ArrayList<T>(names.size());
        for (String name : names) {
            try {
                Class<?> instanceClass = ClassUtils.forName(name, classLoader);
                Assert.isAssignable(type, instanceClass);
                Constructor<?> constructor = instanceClass.getDeclaredConstructor(parameterTypes);
                T instance = (T) BeanUtils.instantiateClass(constructor, args);
                instances.add(instance);
            }
            catch (Throwable ex) {
                throw new IllegalArgumentException("Cannot instantiate " + type + " : " + name, ex);
            }
        }
        return instances;
    }

    private ConfigurableEnvironment getOrCreateEnvironment() {
        if (this.environment != null) {
            return this.environment;
        }
        if (this.webEnvironment) {
            return new StandardServletEnvironment();
        }
        return new StandardEnvironment();
    }

    /**
     * Template method delegating to
     * {@link #configurePropertySources(ConfigurableEnvironment, String[])} and
     * {@link #configureProfiles(ConfigurableEnvironment, String[])} in that order.
     * Override this method for complete control over Environment customization, or one of
     * the above for fine-grained control over property sources or profiles, respectively.
     * @param environment this application's environment
     * @param args arguments passed to the {@code run} method
     * @see #configureProfiles(ConfigurableEnvironment, String[])
     * @see #configurePropertySources(ConfigurableEnvironment, String[])
     */
    protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
        // 配置属性源
        configurePropertySources(environment, args);
        // TODO 配置profiles
        configureProfiles(environment, args);
    }

    /**
     * Add, remove or re-order any {@link PropertySource}s in this application's
     * environment.
     * @param environment this application's environment
     * @param args arguments passed to the {@code run} method
     * @see #configureEnvironment(ConfigurableEnvironment, String[])
     */
    protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
        // 获取Spring环境的属性源对象
        MutablePropertySources sources = environment.getPropertySources();
        // 添加默认属性
        if (this.defaultProperties != null && !this.defaultProperties.isEmpty()) {
            sources.addLast(new MapPropertySource("defaultProperties", this.defaultProperties));
        }
        // 添加args到命令行参数
        if (this.addCommandLineProperties && args.length > 0) {
            String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
            // 如果已经存在命令行参数,不覆盖,添加到组合属性源中(如果有重复参数,按照先后顺序来获取)
            if (sources.contains(name)) {
                PropertySource<?> source = sources.get(name);
                CompositePropertySource composite = new CompositePropertySource(name);
                composite.addPropertySource(new SimpleCommandLinePropertySource(name + "-" + args.hashCode(), args));
                composite.addPropertySource(source);
                sources.replace(name, composite);
            }
            else {
                // 作为命令行参数添加到环境中去
                sources.addFirst(new SimpleCommandLinePropertySource(args));
            }
        }
    }

    /**
     * Configure which profiles are active (or active by default) for this application
     * environment. Additional profiles may be activated during configuration file
     * processing via the {@code spring.profiles.active} property.
     * @param environment this application's environment
     * @param args arguments passed to the {@code run} method
     * @see #configureEnvironment(ConfigurableEnvironment, String[])
     * @see org.springframework.boot.context.config.ConfigFileApplicationListener
     */
    protected void configureProfiles(ConfigurableEnvironment environment, String[] args) {
        // 确保初始化
        environment.getActiveProfiles(); // ensure they are initialized
        // But these ones should go first (last wins in a property key clash)
        // 设置附件的profile
        Set<String> profiles = new LinkedHashSet<String>(this.additionalProfiles);
        // 添加环境中的profile
        profiles.addAll(Arrays.asList(environment.getActiveProfiles()));
        // 设置新的profile
        environment.setActiveProfiles(profiles.toArray(new String[profiles.size()]));
    }

    /**
     * Strategy method used to create the {@link ApplicationContext}. By default this
     * method will respect any explicitly set application context or application context
     * class before falling back to a suitable default.
     * @return the application context (not yet refreshed)
     * @see #setApplicationContextClass(Class)
     */
    protected ConfigurableApplicationContext createApplicationContext() {
        // 初始化spring上下文
        Class<?> contextClass = this.applicationContextClass;
        if (contextClass == null) {
            try {
                // 创建基于annotation的上下文对象(AnnotationConfigApplicationContext)
                contextClass = Class.forName(this.webEnvironment ? DEFAULT_WEB_CONTEXT_CLASS : DEFAULT_CONTEXT_CLASS);
            }
            catch (ClassNotFoundException ex) {
                throw new IllegalStateException(
                        "Unable create a default ApplicationContext, " + "please specify an ApplicationContextClass",
                        ex);
            }
        }
        return (ConfigurableApplicationContext) BeanUtils.instantiate(contextClass);
    }

    /**
     * Apply any relevant post processing the {@link ApplicationContext}. Subclasses can
     * apply additional processing as required.
     * @param context the application context
     */
    protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
        if (this.beanNameGenerator != null) {
            context.getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
                    this.beanNameGenerator);
        }
        if (this.resourceLoader != null) {
            if (context instanceof GenericApplicationContext) {
                ((GenericApplicationContext) context).setResourceLoader(this.resourceLoader);
            }
            if (context instanceof DefaultResourceLoader) {
                ((DefaultResourceLoader) context).setClassLoader(this.resourceLoader.getClassLoader());
            }
        }
    }

    /**
     * Apply any {@link ApplicationContextInitializer}s to the context before it is
     * refreshed.
     * @param context the configured ApplicationContext (not refreshed yet)
     * @see ConfigurableApplicationContext#refresh()
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void applyInitializers(ConfigurableApplicationContext context) {
        for (ApplicationContextInitializer initializer : getInitializers()) {
            Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
                    ApplicationContextInitializer.class);
            Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
            initializer.initialize(context);
        }
    }

    /**
     * Called to log startup information, subclasses may override to add additional
     * logging.
     * @param isRoot true if this application is the root of a context hierarchy
     */
    protected void logStartupInfo(boolean isRoot) {
        if (isRoot) {
            new StartupInfoLogger(this.mainApplicationClass).logStarting(getApplicationLog());
        }
    }

    /**
     * Called to log active profile information.
     * @param context the application context
     */
    protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
        Log log = getApplicationLog();
        if (log.isInfoEnabled()) {
            String[] activeProfiles = context.getEnvironment().getActiveProfiles();
            if (ObjectUtils.isEmpty(activeProfiles)) {
                String[] defaultProfiles = context.getEnvironment().getDefaultProfiles();
                log.info("No active profile set, falling back to default profiles: "
                        + StringUtils.arrayToCommaDelimitedString(defaultProfiles));
            }
            else {
                log.info("The following profiles are active: "
                        + StringUtils.arrayToCommaDelimitedString(activeProfiles));
            }
        }
    }

    /**
     * Returns the {@link Log} for the application. By default will be deduced.
     * @return the application log
     */
    protected Log getApplicationLog() {
        if (this.mainApplicationClass == null) {
            return logger;
        }
        return LogFactory.getLog(this.mainApplicationClass);
    }

    /**
     * Load beans into the application context.
     * @param context the context to load beans into
     * @param sources the sources to load
     */
    protected void load(ApplicationContext context, Object[] sources) {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading source " + StringUtils.arrayToCommaDelimitedString(sources));
        }
        // 创建BeanDefinitionLoader实例
        // 需要注意context需要实现BeanDefinitionRegistry或关联的BeanFactory
        // 为BeanDefinitionRegistry实例
        BeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);
        if (this.beanNameGenerator != null) {
            // 设置beanName生成器
            loader.setBeanNameGenerator(this.beanNameGenerator);
        }
        if (this.resourceLoader != null) {
            loader.setResourceLoader(this.resourceLoader);
        }
        if (this.environment != null) {
            loader.setEnvironment(this.environment);
        }
        // TODO 加载bean资源定义
        loader.load();
    }

    /**
     * The ResourceLoader that will be used in the ApplicationContext.
     * @return the resourceLoader the resource loader that will be used in the
     * ApplicationContext (or null if the default)
     */
    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    /**
     * Either the ClassLoader that will be used in the ApplicationContext (if
     * {@link #setResourceLoader(ResourceLoader) resourceLoader} is set, or the context
     * class loader (if not null), or the loader of the Spring {@link ClassUtils} class.
     * @return a ClassLoader (never null)
     */
    public ClassLoader getClassLoader() {
        if (this.resourceLoader != null) {
            return this.resourceLoader.getClassLoader();
        }
        return ClassUtils.getDefaultClassLoader();
    }

    /**
     * Get the bean definition registry.
     * @param context the application context
     * @return the BeanDefinitionRegistry if it can be determined
     */
    private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
        if (context instanceof BeanDefinitionRegistry) {
            return (BeanDefinitionRegistry) context;
        }
        if (context instanceof AbstractApplicationContext) {
            return (BeanDefinitionRegistry) ((AbstractApplicationContext) context).getBeanFactory();
        }
        throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
    }

    /**
     * Factory method used to create the {@link BeanDefinitionLoader}.
     * @param registry the bean definition registry
     * @param sources the sources to load
     * @return the {@link BeanDefinitionLoader} that will be used to load beans
     */
    protected BeanDefinitionLoader createBeanDefinitionLoader(BeanDefinitionRegistry registry, Object[] sources) {
        return new BeanDefinitionLoader(registry, sources);
    }

    /**
     * Refresh the underlying {@link ApplicationContext}.
     * @param applicationContext the application context to refresh
     */
    protected void refresh(ApplicationContext applicationContext) {
        Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
        ((AbstractApplicationContext) applicationContext).refresh();
    }

    /**
     * Called after the context has been refreshed.
     * @param context the application context
     * @param args the application arguments
     */
    protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {
        callRunners(context, args);
    }

    private void callRunners(ApplicationContext context, ApplicationArguments args) {
        List<Object> runners = new ArrayList<Object>();
        // 获取容器中所有的ApplicationRunner
        runners.addAll(context.getBeansOfType(ApplicationRunner.class).values());
        // 获取容器中所有的CommandLineRunner
        runners.addAll(context.getBeansOfType(CommandLineRunner.class).values());
        AnnotationAwareOrderComparator.sort(runners);
        for (Object runner : new LinkedHashSet<Object>(runners)) {
            if (runner instanceof ApplicationRunner) {
                callRunner((ApplicationRunner) runner, args);
            }
            if (runner instanceof CommandLineRunner) {
                callRunner((CommandLineRunner) runner, args);
            }
        }
    }

    private void callRunner(ApplicationRunner runner, ApplicationArguments args) {
        try {
            (runner).run(args);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to execute ApplicationRunner", ex);
        }
    }

    private void callRunner(CommandLineRunner runner, ApplicationArguments args) {
        try {
            (runner).run(args.getSourceArgs());
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to execute CommandLineRunner", ex);
        }
    }

    private void handleRunFailure(ConfigurableApplicationContext context, SpringApplicationRunListeners listeners,
                                  FailureAnalyzers analyzers, Throwable exception) {
        try {
            try {
                handleExitCode(context, exception);
                listeners.finished(context, exception);
            }
            finally {
                reportFailure(analyzers, exception);
                if (context != null) {
                    context.close();
                }
            }
        }
        catch (Exception ex) {
            logger.warn("Unable to close ApplicationContext", ex);
        }
        ReflectionUtils.rethrowRuntimeException(exception);
    }

    private void reportFailure(FailureAnalyzers analyzers, Throwable failure) {
        try {
            if (analyzers != null && analyzers.analyzeAndReport(failure)) {
                registerLoggedException(failure);
                return;
            }
        }
        catch (Throwable ex) {
            // Continue with normal handling of the original failure
        }
        if (logger.isErrorEnabled()) {
            logger.error("Application startup failed", failure);
            registerLoggedException(failure);
        }
    }

    /**
     * Register that the given exception has been logged. By default, if the running in
     * the main thread, this method will suppress additional printing of the stacktrace.
     * @param exception the exception that was logged
     */
    protected void registerLoggedException(Throwable exception) {
        SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
        if (handler != null) {
            handler.registerLoggedException(exception);
        }
    }

    private void handleExitCode(ConfigurableApplicationContext context, Throwable exception) {
        int exitCode = getExitCodeFromException(context, exception);
        if (exitCode != 0) {
            if (context != null) {
                context.publishEvent(new ExitCodeEvent(context, exitCode));
            }
            SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
            if (handler != null) {
                handler.registerExitCode(exitCode);
            }
        }
    }

    private int getExitCodeFromException(ConfigurableApplicationContext context, Throwable exception) {
        int exitCode = getExitCodeFromMappedException(context, exception);
        if (exitCode == 0) {
            exitCode = getExitCodeFromExitCodeGeneratorException(exception);
        }
        return exitCode;
    }

    private int getExitCodeFromMappedException(ConfigurableApplicationContext context, Throwable exception) {
        if (context == null || !context.isActive()) {
            return 0;
        }
        ExitCodeGenerators generators = new ExitCodeGenerators();
        Collection<ExitCodeExceptionMapper> beans = context.getBeansOfType(ExitCodeExceptionMapper.class).values();
        generators.addAll(exception, beans);
        return generators.getExitCode();
    }

    private int getExitCodeFromExitCodeGeneratorException(Throwable exception) {
        if (exception == null) {
            return 0;
        }
        if (exception instanceof ExitCodeGenerator) {
            return ((ExitCodeGenerator) exception).getExitCode();
        }
        return getExitCodeFromExitCodeGeneratorException(exception.getCause());
    }

    SpringBootExceptionHandler getSpringBootExceptionHandler() {
        if (isMainThread(Thread.currentThread())) {
            return SpringBootExceptionHandler.forCurrentThread();
        }
        return null;
    }

    private boolean isMainThread(Thread currentThread) {
        return ("main".equals(currentThread.getName()) || "restartedMain".equals(currentThread.getName()))
                && "main".equals(currentThread.getThreadGroup().getName());
    }

    /**
     * Returns the main application class that has been deduced or explicitly configured.
     * @return the main application class or {@code null}
     */
    public Class<?> getMainApplicationClass() {
        return this.mainApplicationClass;
    }

    /**
     * Set a specific main application class that will be used as a log source and to
     * obtain version information. By default the main application class will be deduced.
     * Can be set to {@code null} if there is no explicit application class.
     * @param mainApplicationClass the mainApplicationClass to set or {@code null}
     */
    public void setMainApplicationClass(Class<?> mainApplicationClass) {
        this.mainApplicationClass = mainApplicationClass;
    }

    /**
     * Returns whether this {@link SpringApplication} is running within a web environment.
     * @return {@code true} if running within a web environment, otherwise {@code false}.
     * @see #setWebEnvironment(boolean)
     */
    public boolean isWebEnvironment() {
        return this.webEnvironment;
    }

    /**
     * Sets if this application is running within a web environment. If not specified will
     * attempt to deduce the environment based on the classpath.
     * @param webEnvironment if the application is running in a web environment
     */
    public void setWebEnvironment(boolean webEnvironment) {
        this.webEnvironment = webEnvironment;
    }

    /**
     * Sets if the application is headless and should not instantiate AWT. Defaults to
     * {@code true} to prevent java icons appearing.
     * @param headless if the application is headless
     */
    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    /**
     * Sets if the created {@link ApplicationContext} should have a shutdown hook
     * registered. Defaults to {@code true} to ensure that JVM shutdowns are handled
     * gracefully.
     * @param registerShutdownHook if the shutdown hook should be registered
     */
    public void setRegisterShutdownHook(boolean registerShutdownHook) {
        this.registerShutdownHook = registerShutdownHook;
    }

    /**
     * Sets if the application information should be logged when the application starts.
     * Defaults to {@code true}.
     * @param logStartupInfo if startup info should be logged.
     */
    public void setLogStartupInfo(boolean logStartupInfo) {
        this.logStartupInfo = logStartupInfo;
    }

    /**
     * Sets if a {@link CommandLinePropertySource} should be added to the application
     * context in order to expose arguments. Defaults to {@code true}.
     * @param addCommandLineProperties if command line arguments should be exposed
     */
    public void setAddCommandLineProperties(boolean addCommandLineProperties) {
        this.addCommandLineProperties = addCommandLineProperties;
    }

    /**
     * Set default environment properties which will be used in addition to those in the
     * existing {@link Environment}.
     * @param defaultProperties the additional properties to set
     */
    public void setDefaultProperties(Map<String, Object> defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    /**
     * Convenient alternative to {@link #setDefaultProperties(Map)}.
     * @param defaultProperties some {@link Properties}
     */
    public void setDefaultProperties(Properties defaultProperties) {
        this.defaultProperties = new HashMap<String, Object>();
        for (Object key : Collections.list(defaultProperties.propertyNames())) {
            this.defaultProperties.put((String) key, defaultProperties.get(key));
        }
    }

    /**
     * Set additional profile values to use (on top of those set in system or command line
     * properties).
     * @param profiles the additional profiles to set
     */
    public void setAdditionalProfiles(String... profiles) {
        this.additionalProfiles = new LinkedHashSet<String>(Arrays.asList(profiles));
    }

    /**
     * Sets the bean name generator that should be used when generating bean names.
     * @param beanNameGenerator the bean name generator
     */
    public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
        this.beanNameGenerator = beanNameGenerator;
    }

    /**
     * Sets the underlying environment that should be used with the created application
     * context.
     * @param environment the environment
     */
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Returns a mutable set of the sources that will be added to an ApplicationContext
     * when {@link #run(String...)} is called.
     * @return the sources the application sources.
     * @see #SpringApplication(Object...)
     */
    public Set<Object> getSources() {
        return this.sources;
    }

    /**
     * The sources that will be used to create an ApplicationContext. A valid source is
     * one of: a class, class name, package, package name, or an XML resource location.
     * Can also be set using constructors and static convenience methods (e.g.
     * {@link #run(Object[], String[])}).
     * <p>
     * NOTE: sources defined here will be used in addition to any sources specified on
     * construction.
     * @param sources the sources to set
     * @see #SpringApplication(Object...)
     */
    public void setSources(Set<Object> sources) {
        Assert.notNull(sources, "Sources must not be null");
        this.sources.addAll(sources);
    }

    /**
     * Sets the {@link ResourceLoader} that should be used when loading resources.
     * @param resourceLoader the resource loader
     */
    public void setResourceLoader(ResourceLoader resourceLoader) {
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.resourceLoader = resourceLoader;
    }

    /**
     * Sets the type of Spring {@link ApplicationContext} that will be created. If not
     * specified defaults to {@link #DEFAULT_WEB_CONTEXT_CLASS} for web based applications
     * or {@link AnnotationConfigApplicationContext} for non web based applications.
     * @param applicationContextClass the context class to set
     */
    public void setApplicationContextClass(Class<? extends ConfigurableApplicationContext> applicationContextClass) {
        this.applicationContextClass = applicationContextClass;
        if (!isWebApplicationContext(applicationContextClass)) {
            this.webEnvironment = false;
        }
    }

    private boolean isWebApplicationContext(Class<?> applicationContextClass) {
        try {
            return WebApplicationContext.class.isAssignableFrom(applicationContextClass);
        }
        catch (NoClassDefFoundError ex) {
            return false;
        }
    }

    /**
     * Sets the {@link ApplicationContextInitializer} that will be applied to the Spring
     * {@link ApplicationContext}.
     * @param initializers the initializers to set
     */
    public void setInitializers(Collection<? extends ApplicationContextInitializer<?>> initializers) {
        this.initializers = new ArrayList<ApplicationContextInitializer<?>>();
        this.initializers.addAll(initializers);
    }

    /**
     * Add {@link ApplicationContextInitializer}s to be applied to the Spring
     * {@link ApplicationContext}.
     * @param initializers the initializers to add
     */
    public void addInitializers(ApplicationContextInitializer<?>... initializers) {
        this.initializers.addAll(Arrays.asList(initializers));
    }

    /**
     * Returns read-only ordered Set of the {@link ApplicationContextInitializer}s that
     * will be applied to the Spring {@link ApplicationContext}.
     * @return the initializers
     */
    public Set<ApplicationContextInitializer<?>> getInitializers() {
        return asUnmodifiableOrderedSet(this.initializers);
    }

    /**
     * Sets the {@link ApplicationListener}s that will be applied to the SpringApplication
     * and registered with the {@link ApplicationContext}.
     * @param listeners the listeners to set
     */
    public void setListeners(Collection<? extends ApplicationListener<?>> listeners) {
        this.listeners = new ArrayList<ApplicationListener<?>>();
        this.listeners.addAll(listeners);
    }

    /**
     * Add {@link ApplicationListener}s to be applied to the SpringApplication and
     * registered with the {@link ApplicationContext}.
     * @param listeners the listeners to add
     */
    public void addListeners(ApplicationListener<?>... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
    }

    /**
     * Returns read-only ordered Set of the {@link ApplicationListener}s that will be
     * applied to the SpringApplication and registered with the {@link ApplicationContext}
     * .
     * @return the listeners
     */
    public Set<ApplicationListener<?>> getListeners() {
        return asUnmodifiableOrderedSet(this.listeners);
    }

    /**
     * Static helper that can be used to run a {@link SpringApplication} from the
     * specified source using default settings.
     * @param source the source to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Object source, String... args) {
        return run(new Object[] { source }, args);
    }

    /**
     * Static helper that can be used to run a {@link SpringApplication} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
        return new SpringApplication(sources).run(args);
    }

    /**
     * A basic main that can be used to launch an application. This method is useful when
     * application sources are defined via a {@literal --spring.main.sources} command line
     * argument.
     * <p>
     * Most developers will want to define their own main method and call the
     * {@link #run(Object, String...) run} method instead.
     * @param args command line arguments
     * @throws Exception if the application cannot be started
     * @see SpringApplication#run(Object[], String[])
     * @see SpringApplication#run(Object, String...)
     */
    public static void main(String[] args) throws Exception {
        SpringApplication.run(new Object[0], args);
    }

    /**
     * Static helper that can be used to exit a {@link SpringApplication} and obtain a
     * code indicating success (0) or otherwise. Does not throw exceptions but should
     * print stack traces of any encountered. Applies the specified
     * {@link ExitCodeGenerator} in addition to any Spring beans that implement
     * {@link ExitCodeGenerator}. In the case of multiple exit codes the highest value
     * will be used (or if all values are negative, the lowest value will be used)
     * @param context the context to close if possible
     * @param exitCodeGenerators exist code generators
     * @return the outcome (0 if successful)
     */
    public static int exit(ApplicationContext context, ExitCodeGenerator... exitCodeGenerators) {
        Assert.notNull(context, "Context must not be null");
        int exitCode = 0;
        try {
            try {
                ExitCodeGenerators generators = new ExitCodeGenerators();
                Collection<ExitCodeGenerator> beans = context.getBeansOfType(ExitCodeGenerator.class).values();
                generators.addAll(exitCodeGenerators);
                generators.addAll(beans);
                exitCode = generators.getExitCode();
                if (exitCode != 0) {
                    context.publishEvent(new ExitCodeEvent(context, exitCode));
                }
            }
            finally {
                close(context);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            exitCode = (exitCode != 0) ? exitCode : 1;
        }
        return exitCode;
    }

    private static void close(ApplicationContext context) {
        if (context instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext closable = (ConfigurableApplicationContext) context;
            closable.close();
        }
    }

    private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {
        List<E> list = new ArrayList<E>();
        list.addAll(elements);
        Collections.sort(list, AnnotationAwareOrderComparator.INSTANCE);
        return new LinkedHashSet<E>(list);
    }

}
