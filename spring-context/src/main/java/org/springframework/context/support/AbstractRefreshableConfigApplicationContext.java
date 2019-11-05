package org.springframework.context.support;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractRefreshableApplicationContext} subclass that adds common handling
 * of specified config locations. Serves as base class for XML-based application
 * context implementations such as {@link ClassPathXmlApplicationContext} and
 * {@link FileSystemXmlApplicationContext}, as well as
 * {@link org.springframework.web.context.support.XmlWebApplicationContext} and
 * {@link org.springframework.web.portlet.context.XmlPortletApplicationContext}.
 *
 * @author Juergen Hoeller
 * @see #setConfigLocation
 * @see #setConfigLocations
 * @see #getDefaultConfigLocations
 * @since 2.5.2
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext {

    private String[] configLocations;

    public AbstractRefreshableConfigApplicationContext() {
    }

    public AbstractRefreshableConfigApplicationContext(ApplicationContext parent) {
        super(parent);
    }

    /**
     * Set the config locations for this application context in init-param style,
     * i.e. with distinct locations separated by commas, semicolons or whitespace.
     * <p>If not set, the implementation may use a default as appropriate.
     */
    public void setConfigLocation(String location) {
        setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
    }

    /**
     * Set the config locations for this application context.
     * <p>If not set, the implementation may use a default as appropriate.
     */
    public void setConfigLocations(String... locations) {
        if (locations != null) {
            Assert.noNullElements(locations, "Config locations must not be null");
            this.configLocations = new String[locations.length];
            for (int i = 0; i < locations.length; i++) {
                this.configLocations[i] = resolvePath(locations[i]).trim();
            }
        } else {
            this.configLocations = null;
        }
    }

    protected String[] getConfigLocations() {
        return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
    }

    protected String[] getDefaultConfigLocations() {
        return null;
    }

    protected String resolvePath(String path) {
        return getEnvironment().resolveRequiredPlaceholders(path);
    }


}
