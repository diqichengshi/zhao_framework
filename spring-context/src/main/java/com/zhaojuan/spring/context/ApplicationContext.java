package com.zhaojuan.spring.context;

public interface ApplicationContext {
    /**
     * 根据传入的bean的id值获取容器中的对象,类型为Object
     */
    Object getBean(String beanId);
    /**
     * 销毁方法，用于释放资源
     */
    void destroy();
}
