package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.TargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.beans.PropertyDescriptor;

public class InfrastructureAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator{

    private ConfigurableListableBeanFactory beanFactory;

    @Override
    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.initBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    protected boolean isEligibleAdvisorBean(String beanName) {
        return (this.beanFactory.containsBeanDefinition(beanName) &&
                this.beanFactory.getBeanDefinition(beanName).getRole() == BeanDefinition.ROLE_INFRASTRUCTURE);
    }

}
