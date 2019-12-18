package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

public abstract class AbstractGenericPointcutAdvisor extends AbstractPointcutAdvisor{
   
    private static final long serialVersionUID = 8508282209933397324L;
    
    private Advice advice;


    /**
     * Specify the advice that this advisor should apply.
     */
    public void setAdvice(Advice advice) {
        this.advice = advice;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }


    @Override
    public String toString() {
        return getClass().getName() + ": advice [" + getAdvice() + "]";
    }
}
