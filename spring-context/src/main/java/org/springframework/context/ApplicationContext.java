package org.springframework.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;

public interface ApplicationContext extends EnvironmentCapable,ListableBeanFactory {
    /**
     * 根据传入的bean的id值获取容器中的对象,类型为Object
     */
    public Object getBean(String name) throws BeansException;

}
