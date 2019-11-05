package org.springframework.context;

import org.springframework.beans.BeansException;

public interface ConfigurableApplicationContext extends ApplicationContext {

    String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

    public void refresh() throws BeansException, IllegalStateException;

}
