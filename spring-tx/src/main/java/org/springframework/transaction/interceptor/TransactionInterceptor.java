package org.springframework.transaction.interceptor;
/**
 * AOP Alliance MethodInterceptor for declarative transaction
 * management using the common Spring transaction infrastructure
 * ({@link org.springframework.transaction.PlatformTransactionManager}).
 *
 * <p>Derives from the {@link TransactionAspectSupport} class which
 * contains the integration with Spring's underlying transaction API.
 * TransactionInterceptor simply calls the relevant superclass methods
 * such as {@link #invokeWithinTransaction} in the correct order.
 *
 * <p>TransactionInterceptors are thread-safe.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see TransactionProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactory
 */
public class TransactionInterceptor {
}
