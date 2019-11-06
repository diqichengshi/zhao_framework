package org.springframework.context;

import org.springframework.beans.BeansException;

public interface ConfigurableApplicationContext extends ApplicationContext {
    /**
     * Any number of these characters are considered delimiters between
     * multiple context config paths in a single String value.
     * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
     * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
     * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
     */
    String CONFIG_LOCATION_DELIMITERS = ",; \t\n";
    /**
     * Name of the ConversionService bean in the factory.
     * If none is supplied, default conversion rules apply.
     * @see org.springframework.core.convert.ConversionService
     */
    String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

    /**
     * Name of the LoadTimeWeaver bean in the factory. If such a bean is supplied,
     * the context will use a temporary ClassLoader for type matching, in order
     * to allow the LoadTimeWeaver to process all actual bean classes.
     * @see org.springframework.instrument.classloading.LoadTimeWeaver
     */
    String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

    /**
     * Name of the {@link Environment} bean in the factory.
     */
    String ENVIRONMENT_BEAN_NAME = "environment";

    /**
     * Name of the System properties bean in the factory.
     * @see java.lang.System#getProperties()
     */
    String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

    /**
     * Name of the System environment bean in the factory.
     * @see java.lang.System#getenv()
     */
    String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";
    public void refresh() throws BeansException, IllegalStateException;

}
