package org.springframework.context;

import org.springframework.beans.exception.BeansException;
import org.springframework.beans.factory.BeanFactory;

public interface ApplicationContext extends BeanFactory {
    /**
     * 根据传入的bean的id值获取容器中的对象,类型为Object
     */
    public Object getBean(String name) throws BeansException;

}
