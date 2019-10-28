package org.springframework.context;

import org.springframework.beans.exception.BeansException;

public interface ConfigurableApplicationContext extends ApplicationContext{

    public void refresh() throws BeansException, IllegalStateException;
}
