package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * Interface to be implemented by classes that hold the configuration
 * of a factory of AOP proxies. This configuration includes the
 * Interceptors and other advice, Advisors, and the proxied interfaces.
 *
 * <p>Any AOP proxy obtained from Spring can be cast to this interface to
 * allow manipulation of its AOP advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.framework.AdvisedSupport
 * @since 13.03.2003
 */
public interface Advised extends TargetClassAware {
    /**
     * Return whether the Advised configuration is frozen,
     * in which case no advice changes can be made.
     */
    boolean isFrozen();
    /**
     * Return the interfaces proxied by the AOP proxy.
     * <p>Will not include the target class, which may also be proxied.
     */
    Class<?>[] getProxiedInterfaces();

    /**
     * Determine whether the given interface is proxied.
     *
     * @param intf the interface to check
     */
    boolean isInterfaceProxied(Class<?> intf);

    /**
     * Change the {@code TargetSource} used by this {@code Advised} object.
     * <p>Only works if the configuration isn't {@linkplain #isFrozen frozen}.
     *
     * @param targetSource new TargetSource to use
     */
    void setTargetSource(TargetSource targetSource);

    /**
     * Return the {@code TargetSource} used by this {@code Advised} object.
     */
    TargetSource getTargetSource();

    /**
     * Set whether this proxy configuration is pre-filtered so that it only
     * contains applicable advisors (matching this proxy's target class).
     * <p>Default is "false". Set this to "true" if the advisors have been
     * pre-filtered already, meaning that the ClassFilter check can be skipped
     * when building the actual advisor chain for proxy invocations.
     *
     * @see org.springframework.aop.ClassFilter
     */
    void setPreFiltered(boolean preFiltered);

    /**
     * Return whether this proxy configuration is pre-filtered so that it only
     * contains applicable advisors (matching this proxy's target class).
     */
    boolean isPreFiltered();

    /**
     * Return the advisors applying to this proxy.
     *
     * @return a list of Advisors applying to this proxy (never {@code null})
     */
    Advisor[] getAdvisors();

    /**
     * Add an advisor at the end of the advisor chain.
     * <p>The Advisor may be an {@link org.springframework.aop.IntroductionAdvisor},
     * in which new interfaces will be available when a proxy is next obtained
     * from the relevant factory.
     *
     * @param advisor the advisor to add to the end of the chain
     * @throws AopConfigException in case of invalid advice
     */
    void addAdvisor(Advisor advisor) throws AopConfigException;

    /**
     * Add an Advisor at the specified position in the chain.
     *
     * @param advisor the advisor to add at the specified position in the chain
     * @param pos     position in chain (0 is head). Must be valid.
     * @throws AopConfigException in case of invalid advice
     */
    void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

    /**
     * Remove the given advisor.
     *
     * @param advisor the advisor to remove
     * @return {@code true} if the advisor was removed; {@code false}
     * if the advisor was not found and hence could not be removed
     */
    boolean removeAdvisor(Advisor advisor);

    /**
     * Remove the advisor at the given index.
     *
     * @param index index of advisor to remove
     * @throws AopConfigException if the index is invalid
     */
    void removeAdvisor(int index) throws AopConfigException;

    /**
     * Return the index (from 0) of the given advisor,
     * or -1 if no such advisor applies to this proxy.
     * <p>The return value of this method can be used to index into the advisors array.
     *
     * @param advisor the advisor to search for
     * @return index from 0 of this advisor, or -1 if there's no such advisor
     */
    int indexOf(Advisor advisor);

    /**
     * Add the given AOP Alliance advice to the tail of the advice (interceptor) chain.
     * <p>This will be wrapped in a DefaultPointcutAdvisor with a pointcut that always
     * applies, and returned from the {@code getAdvisors()} method in this wrapped form.
     * <p>Note that the given advice will apply to all invocations on the proxy,
     * even to the {@code toString()} method! Use appropriate advice implementations
     * or specify appropriate pointcuts to apply to a narrower set of methods.
     *
     * @param advice advice to add to the tail of the chain
     * @throws AopConfigException in case of invalid advice
     * @see #addAdvice(int, Advice)
     * @see org.springframework.aop.support.DefaultPointcutAdvisor
     */
    void addAdvice(Advice advice) throws AopConfigException;

    /**
     * Add the given AOP Alliance Advice at the specified position in the advice chain.
     * <p>This will be wrapped in a {@link org.springframework.aop.support.DefaultPointcutAdvisor}
     * with a pointcut that always applies, and returned from the {@link #getAdvisors()}
     * method in this wrapped form.
     * <p>Note: The given advice will apply to all invocations on the proxy,
     * even to the {@code toString()} method! Use appropriate advice implementations
     * or specify appropriate pointcuts to apply to a narrower set of methods.
     *
     * @param pos    index from 0 (head)
     * @param advice advice to add at the specified position in the advice chain
     * @throws AopConfigException in case of invalid advice
     */
    void addAdvice(int pos, Advice advice) throws AopConfigException;
}
