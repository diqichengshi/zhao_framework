package org.springframework.beans.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;

import java.util.List;

public interface BeanDefinition {
    /**
     * Scope identifier for the standard singleton scope: "singleton".
     * <p>Note that extended bean factories might support further scopes.
     * @see #setScope
     */
    final static String SCOPE_SINGLETON = "singleton";
    /**
     * Scope identifier for the standard prototype scope: "prototype".
     * <p>Note that extended bean factories might support further scopes.
     * @see #setScope
     */
    final static String SCOPE_PROTOTYPE = "prototype";

    public String getBeanName();

    public void setBeanName(String beanName);

    Class<?> getBeanClass();

    String getScope();

    void setScope(String scope);

    boolean isSingleton();

    boolean isPrototype();

    String getInitMethodName();

    public String[] getDependsOn();

    public void setDependsOn(List<String> refList);

    public void setDependsOn(String[] dependsOn);

    MutablePropertyValues getPropertyValues();

    public void setAutowireMode(int autowireMode);

    public int getResolvedAutowireMode();

}
