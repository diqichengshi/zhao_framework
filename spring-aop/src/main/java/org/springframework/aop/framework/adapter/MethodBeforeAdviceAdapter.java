package org.springframework.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;

import java.io.Serializable;
/**
 * Adapter to enable {@link org.springframework.aop.MethodBeforeAdvice}
 * to be used in the Spring AOP framework.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class MethodBeforeAdviceAdapter implements AdvisorAdapter, Serializable {

    private static final long serialVersionUID = 1227139454465040612L;

    @Override
     public boolean supportsAdvice(Advice advice) {
         return (advice instanceof MethodBeforeAdvice);
     }

     @Override
     public MethodInterceptor getInterceptor(Advisor advisor) {
         MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
         return new MethodBeforeAdviceInterceptor(advice);
     }

 }
