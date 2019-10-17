package com.zhaojuan.spring.beans;

import com.zhaojuan.spring.core.util.BeanUtils;
import com.zhaojuan.spring.core.util.ClassUtils;
import com.zhaojuan.spring.core.util.ReflectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class AbstractBeanFactory {
    protected final Log logger = LogFactory.getLog(getClass());
    /**
     * 实例化bean
     */
    protected BeanWrapper instantiateBean(final String beanName, final BeanDefinition bd) {
        try {
            Object beanInstance;

            beanInstance = bd.getBeanClass().newInstance();
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw);
            return bw;
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new BeansException(beanName + " Instantiation of bean failed", ex);
        }
    }

    /**
     * 初始化BeanWrapper,此处暂不处理
     */
    private void initBeanWrapper(BeanWrapper bw) {
    }

    protected Object initializeBean(final String beanName, final Object bean, BeanDefinition mbd) {

        Object wrappedBean = bean;
        // 前置处理器处理  TODO
        /*if (mbd == null ) {
            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        }*/

        try {
            invokeInitMethods(beanName, wrappedBean, mbd);
        } catch (Throwable ex) {
            throw new BeansException(beanName + "Invocation of init method failed", ex);
        }
        // 后置处理器处理  TODO
        /*if (mbd == null ) {
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        }*/
        return wrappedBean;
    }
    /**调用初始化方法*/
    protected void invokeInitMethods(String beanName, final Object bean, BeanDefinition mbd)
            throws Throwable {

        if (mbd != null) {
            String initMethodName = mbd.getInitMethodName();
            if (initMethodName != null && !"afterPropertiesSet".equals(initMethodName)) {
                invokeCustomInitMethod(beanName, bean, mbd);
            }
        }
    }
    /**调用自定义的初始化方法*/
    protected void invokeCustomInitMethod(String beanName, final Object bean, BeanDefinition mbd) throws Throwable {
        String initMethodName = mbd.getInitMethodName();
        final Method initMethod = ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName);
        if (initMethod == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No default init method named '" + initMethodName +
                        "' found on bean with name '" + beanName + "'");
            }
            // Ignore non-existent default lifecycle methods.
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
        }

        try {
            ReflectionUtils.makeAccessible(initMethod);
            initMethod.invoke(bean);
        }
        catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }


}
