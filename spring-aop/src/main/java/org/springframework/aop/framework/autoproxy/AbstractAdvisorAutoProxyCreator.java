package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.List;

public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

    private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalStateException("Cannot use AdvisorAutoProxyCreator without a ConfigurableListableBeanFactory");
        }
        initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
    }

    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
    }

    /**
     * 如果存在增强方法则创建代理(意思就是如果该类有advice则创建proxy)
     */
    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        // 获取合格的增强器
        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
        if (advisors.isEmpty()) {
            return DO_NOT_PROXY;
        }
        return advisors.toArray();
    }


    /**
     * Find all eligible Advisors for auto-proxying this class.
     *
     * @param beanClass the clazz to find advisors for
     * @param beanName  the name of the currently proxied bean
     * @return the empty List, not {@code null},
     * if there are no pointcuts or interceptors
     * @see #findCandidateAdvisors
     * @see #sortAdvisors
     * @see #extendAdvisors
     */
    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        // TODO 获取所有的候选增强器(该方法被子类覆盖)
        List<Advisor> candidateAdvisors = findCandidateAdvisors();
        // TODO 寻找匹配的增强器
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        extendAdvisors(eligibleAdvisors); //  扩展Advisors,添加一个ExposeInvocationInterceptor拦截器
        if (!eligibleAdvisors.isEmpty()) {
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }

    /**
     * Find all candidate Advisors to use in auto-proxying.
     * 获取增强器,该方法被子类覆盖(请查看子类方法)
     * @return the List of candidate Advisors
     */
    protected List<Advisor> findCandidateAdvisors() {
        return this.advisorRetrievalHelper.findAdvisorBeans();
    }

    /**
     * Search the given candidate Advisors to find all Advisors that
     * can apply to the specified bean.
     *
     * @param candidateAdvisors the candidate Advisors
     * @param beanClass         the target's bean class
     * @param beanName          the target's bean name
     * @return the List of applicable Advisors
     * @see ProxyCreationContext#getCurrentProxiedBeanName()
     */
    protected List<Advisor> findAdvisorsThatCanApply(
            List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
        try {
            // 过滤已经得到的advisors
            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        } finally {
            ProxyCreationContext.setCurrentProxiedBeanName(null);
        }
    }

    /**
     * Return whether the Advisor bean with the given name is eligible
     * for proxying in the first place.
     *
     * @param beanName the name of the Advisor bean
     * @return whether the bean is eligible
     */
    protected boolean isEligibleAdvisorBean(String beanName) {
        return true;
    }

    /**
     * Sort advisors based on ordering. Subclasses may choose to override this
     * method to customize the sorting strategy.
     *
     * @param advisors the source List of Advisors
     * @return the sorted List of Advisors
     * @see org.springframework.core.Ordered
     * @see org.springframework.core.annotation.Order
     * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
     */
    protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
        AnnotationAwareOrderComparator.sort(advisors);
        return advisors;
    }

    /**
     * Extension hook that subclasses can override to register additional Advisors,
     * given the sorted Advisors obtained to date.
     * <p>The default implementation is empty.
     * <p>Typically used to add Advisors that expose contextual information
     * required by some of the later advisors.
     *
     * @param candidateAdvisors Advisors that have already been identified as
     *                          applying to a given bean
     */
    protected void extendAdvisors(List<Advisor> candidateAdvisors) {
        // 该方法被子类重写
    }

    /**
     * Subclass of BeanFactoryAdvisorRetrievalHelper that delegates to
     * surrounding AbstractAdvisorAutoProxyCreator facilities.
     */
    private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

        public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
            super(beanFactory);
        }

        @Override
        protected boolean isEligibleBean(String beanName) {
            return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
        }
    }


}
