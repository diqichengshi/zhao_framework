package org.springframework.transaction.interceptor;

import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;

/**
 * Advisor driven by a {@link TransactionAttributeSource}, used to include
 * a {@link TransactionInterceptor} only for methods that are transactional.
 *
 * <p>Because the AOP framework caches advice calculations, this is normally
 * faster than just letting the TransactionInterceptor run and find out
 * itself that it has no work to do.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setTransactionInterceptor
 * @see TransactionProxyFactoryBean
 */
public class TransactionAttributeSourceAdvisor extends AbstractPointcutAdvisor {
    private TransactionInterceptor transactionInterceptor;

    private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
        @Override
        protected TransactionAttributeSource getTransactionAttributeSource() {
            return (transactionInterceptor != null ? transactionInterceptor.getTransactionAttributeSource() : null);
        }
    };


    /**
     * Create a new TransactionAttributeSourceAdvisor.
     */
    public TransactionAttributeSourceAdvisor() {
    }

    /**
     * Create a new TransactionAttributeSourceAdvisor.
     *
     * @param interceptor the transaction interceptor to use for this advisor
     */
    public TransactionAttributeSourceAdvisor(TransactionInterceptor interceptor) {
        setTransactionInterceptor(interceptor);
    }


    /**
     * Set the transaction interceptor to use for this advisor.
     */
    public void setTransactionInterceptor(TransactionInterceptor interceptor) {
        this.transactionInterceptor = interceptor;
    }

    /**
     * Set the {@link ClassFilter} to use for this pointcut.
     * Default is {@link ClassFilter#TRUE}.
     */
    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }


    @Override
    public Advice getAdvice() {
        return this.transactionInterceptor;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }
}
