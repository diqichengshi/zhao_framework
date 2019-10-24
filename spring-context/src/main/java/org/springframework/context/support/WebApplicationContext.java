package org.springframework.context.support;

import org.springframework.context.ApplicationContext;

public class WebApplicationContext implements ApplicationContext {
    public Object getBean(String beanId) {
        return null;
    }

    /**
     * 销毁方法，用于释放资源
     */
    public void destroy() {
    }
}
