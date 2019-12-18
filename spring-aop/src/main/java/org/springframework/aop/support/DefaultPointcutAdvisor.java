package org.springframework.aop.support;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;

public class DefaultPointcutAdvisor extends AbstractGenericPointcutAdvisor{

    private static final long serialVersionUID = 8508282209933397324L;
    
    private Pointcut pointcut = Pointcut.TRUE;

    public DefaultPointcutAdvisor(Advice advice) {
        this(Pointcut.TRUE, advice);
    }

    public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
        this.pointcut = pointcut;
        setAdvice(advice);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
    }

}
