package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;
import org.springframework.aop.*;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdvisedSupport extends ProxyConfig implements Advised{

    public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;
    TargetSource targetSource = EMPTY_TARGET_SOURCE;
    private boolean preFiltered = false;
    private transient Map<MethodCacheKey, List<Object>> methodCache;
    AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();
    private List<Class<?>> interfaces = new ArrayList<Class<?>>();
    private List<Advisor> advisors = new LinkedList<Advisor>();
    private Advisor[] advisorArray = new Advisor[0];

    public AdvisedSupport() {
        initMethodCache();
    }

    public AdvisedSupport(Class<?>[] interfaces) {
        this();
        setInterfaces(interfaces);
    }

    private void initMethodCache() {
        this.methodCache = new ConcurrentHashMap<MethodCacheKey, List<Object>>(32);
    }

    /**
     * Set the given object as target.
     * Will create a SingletonTargetSource for the object.
     * @see #setTargetSource
     * @see org.springframework.aop.target.SingletonTargetSource
     */
    public void setTarget(Object target) {
        setTargetSource(new SingletonTargetSource(target));
    }

    @Override
    public void setTargetSource(TargetSource targetSource) {
        this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
    }

    @Override
    public TargetSource getTargetSource() {
        return this.targetSource;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetSource = EmptyTargetSource.forClass(targetClass);
    }

    @Override
    public Class<?> getTargetClass() {
        return this.targetSource.getTargetClass();
    }
    @Override
    public void setPreFiltered(boolean preFiltered) {
        this.preFiltered = preFiltered;
    }

    @Override
    public boolean isPreFiltered() {
        return this.preFiltered;
    }
    /**
     * Set the interfaces to be proxied.
     */
    public void setInterfaces(Class<?>... interfaces) {
        Assert.notNull(interfaces, "Interfaces must not be null");
        this.interfaces.clear();
        for (Class<?> ifc : interfaces) {
            addInterface(ifc);
        }
    }

    public void addInterface(Class<?> intf) {
        Assert.notNull(intf, "Interface must not be null");
        if (!intf.isInterface()) {
            throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
        }
        if (!this.interfaces.contains(intf)) {
            this.interfaces.add(intf);
            adviceChanged();
        }
    }
    /**
     * Remove a proxied interface.
     * <p>Does nothing if the given interface isn't proxied.
     * @param intf the interface to remove from the proxy
     * @return {@code true} if the interface was removed; {@code false}
     * if the interface was not found and hence could not be removed
     */
    public boolean removeInterface(Class<?> intf) {
        return this.interfaces.remove(intf);
    }

    @Override
    public Class<?>[] getProxiedInterfaces() {
        return this.interfaces.toArray(new Class<?>[this.interfaces.size()]);
    }

    @Override
    public boolean isInterfaceProxied(Class<?> intf) {
        for (Class<?> proxyIntf : this.interfaces) {
            if (intf.isAssignableFrom(proxyIntf)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Advisor[] getAdvisors() {
        return this.advisorArray;
    }

    @Override
    public void addAdvisor(Advisor advisor) {
        int pos = this.advisors.size();
        addAdvisor(pos, advisor);
    }

    @Override
    public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
        if (advisor instanceof IntroductionAdvisor) {
            validateIntroductionAdvisor((IntroductionAdvisor) advisor);
        }
        addAdvisorInternal(pos, advisor);
    }

    @Override
    public boolean removeAdvisor(Advisor advisor) {
        int index = indexOf(advisor);
        if (index == -1) {
            return false;
        }
        else {
            removeAdvisor(index);
            return true;
        }
    }

    @Override
    public void removeAdvisor(int index) throws AopConfigException {
        if (isFrozen()) {
            throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
        }
        if (index < 0 || index > this.advisors.size() - 1) {
            throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
                    "This configuration only has " + this.advisors.size() + " advisors.");
        }

        Advisor advisor = this.advisors.get(index);
        if (advisor instanceof IntroductionAdvisor) {
            IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
            // We need to remove introduction interfaces.
            for (int j = 0; j < ia.getInterfaces().length; j++) {
                removeInterface(ia.getInterfaces()[j]);
            }
        }

        this.advisors.remove(index);
        updateAdvisorArray();
        adviceChanged();
    }

    @Override
    public int indexOf(Advisor advisor) {
        Assert.notNull(advisor, "Advisor must not be null");
        return this.advisors.indexOf(advisor);
    }

    private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
        advisor.validateInterfaces();
        // If the advisor passed validation, we can make the change.
        Class<?>[] ifcs = advisor.getInterfaces();
        for (Class<?> ifc : ifcs) {
            addInterface(ifc);
        }
    }

    private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
        Assert.notNull(advisor, "Advisor must not be null");
        if (isFrozen()) {
            throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
        }
        if (pos > this.advisors.size()) {
            throw new IllegalArgumentException(
                    "Illegal position " + pos + " in advisor list with size " + this.advisors.size());
        }
        this.advisors.add(pos, advisor);
        updateAdvisorArray();
        adviceChanged();
    }
    /**
     * Bring the array up to date with the list.
     */
    protected final void updateAdvisorArray() {
        this.advisorArray = this.advisors.toArray(new Advisor[this.advisors.size()]);
    }

    /**
     * Allows uncontrolled access to the {@link List} of {@link Advisor Advisors}.
     * <p>Use with care, and remember to {@link #updateAdvisorArray() refresh the advisor array}
     * and {@link #adviceChanged() fire advice changed events} when making any modifications.
     */
    protected final List<Advisor> getAdvisorsInternal() {
        return this.advisors;
    }

    @Override
    public void addAdvice(Advice advice) throws AopConfigException {
        int pos = this.advisors.size();
        addAdvice(pos, advice);
    }
    /**
     * Cannot add introductions this way unless the advice implements IntroductionInfo.
     */
    @Override
    public void addAdvice(int pos, Advice advice) throws AopConfigException {
        Assert.notNull(advice, "Advice must not be null");
        if (advice instanceof IntroductionInfo) {
            // We don't need an IntroductionAdvisor for this kind of introduction:
            // It's fully self-describing.
            addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
        }
        else if (advice instanceof DynamicIntroductionAdvice) {
            // We need an IntroductionAdvisor for this kind of introduction.
            throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
        }
        else {
            addAdvisor(pos, new DefaultPointcutAdvisor(advice));
        }
    }

    /**
     * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
     * for the given method, based on this configuration.
     * @param method the proxied method
     * @param targetClass the target class
     * @return List of MethodInterceptors (may also include InterceptorAndDynamicMethodMatchers)
     */
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
        MethodCacheKey cacheKey = new MethodCacheKey(method);
        List<Object> cached = this.methodCache.get(cacheKey);
        if (cached == null) {
            cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
                    this, method, targetClass);
            this.methodCache.put(cacheKey, cached);
        }
        return cached;
    }

    protected void adviceChanged() {
        this.methodCache.clear();
    }

    /**
     * Build a configuration-only copy of this AdvisedSupport,
     * replacing the TargetSource
     */
    AdvisedSupport getConfigurationOnlyCopy() {
        AdvisedSupport copy = new AdvisedSupport();
        copy.copyFrom(this);
        copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
        copy.advisorChainFactory = this.advisorChainFactory;
        copy.interfaces = this.interfaces;
        copy.advisors = this.advisors;
        copy.updateAdvisorArray();
        return copy;
    }

    /**
     * Simple wrapper class around a Method. Used as the key when
     * caching methods, for efficient equals and hashCode comparisons.
     */
    private static class MethodCacheKey {

        private final Method method;

        private final int hashCode;

        public MethodCacheKey(Method method) {
            this.method = method;
            this.hashCode = method.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            MethodCacheKey otherKey = (MethodCacheKey) other;
            return (this.method == otherKey.method);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

}
