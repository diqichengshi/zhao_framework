package org.springframework.context;

import org.springframework.beans.BeansException;

public interface ApplicationContext {
    /**
     * 根据传入的bean的id值获取容器中的对象,类型为Object
     */
    public Object getBean(String name) throws BeansException;
}
