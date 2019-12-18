package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@link AspectJAwareAdvisorAutoProxyCreator} subclass that processes all AspectJ
 * annotation aspects in the current application context, as well as Spring Advisors.
 *
 * <p>Any AspectJ annotated classes will automatically be recognized, and their
 * advice applied if Spring AOP's proxy-based model is capable of applying it.
 * This covers method execution joinpoints.
 *
 * <p>If the &lt;aop:include&gt; element is used, only @AspectJ beans with names matched by
 * an include pattern will be considered as defining aspects to use for Spring auto-proxying.
 *
 * <p>Processing of Spring Advisors follows the rules established in
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory
 */
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {

    private List<Pattern> includePatterns;
    private AspectJAdvisorFactory aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory();
    private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;

    /**
     * Set a list of regex patterns, matching eligible @AspectJ bean names.
     * <p>Default is to consider all @AspectJ beans as eligible.
     */
    public void setIncludePatterns(List<String> patterns) {
        this.includePatterns = new ArrayList<Pattern>(patterns.size());
        for (String patternText : patterns) {
            this.includePatterns.add(Pattern.compile(patternText));
        }
    }

    @Override
    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.initBeanFactory(beanFactory);
        this.aspectJAdvisorsBuilder =
                new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
    }

    @Override
    protected List<Advisor> findCandidateAdvisors() {
        // Add all the Spring advisors found according to superclass rules.
        // 获取父类中的其他AOP配置声明
        List<Advisor> advisors = super.findCandidateAdvisors();
        // Build Advisors for all AspectJ aspects in the bean factory.
        // TODO 获取注解方法配置的AOP信息
        advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
        return advisors;
    }

    /**
     * Check whether the given aspect bean is eligible for auto-proxying.
     * <p>If no &lt;aop:include&gt; elements were used then "includePatterns" will be
     * {@code null} and all beans are included. If "includePatterns" is non-null,
     * then one of the patterns must match.
     */
    protected boolean isEligibleAspectBean(String beanName) {
        if (this.includePatterns == null) {
            return true;
        }
        else {
            for (Pattern pattern : this.includePatterns) {
                if (pattern.matcher(beanName).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Subclass of BeanFactoryAspectJAdvisorsBuilderAdapter that delegates to
     * surrounding AnnotationAwareAspectJAutoProxyCreator facilities.
     */
    private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {

        public BeanFactoryAspectJAdvisorsBuilderAdapter(
                ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
            super(beanFactory, advisorFactory);
        }

        @Override
        protected boolean isEligibleBean(String beanName) {
            return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
        }
    }

}
