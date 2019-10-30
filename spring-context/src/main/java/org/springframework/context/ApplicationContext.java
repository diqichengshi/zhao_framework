package org.springframework.context;

import org.springframework.beans.exception.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;

public interface ApplicationContext extends ListableBeanFactory {
    /**
     * 根据传入的bean的id值获取容器中的对象,类型为Object
     */
    public Object getBean(String name) throws BeansException;

}
