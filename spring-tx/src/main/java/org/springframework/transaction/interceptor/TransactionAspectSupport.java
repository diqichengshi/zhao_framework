package org.springframework.transaction.interceptor;


import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.StringUtils;

/**
 * Base class for transactional aspects, such as the {@link TransactionInterceptor}
 * or an AspectJ aspect.
 *
 * <p>This enables the underlying Spring transaction infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling methods in this class in the correct order.
 *
 * <p>If no transaction name has been specified in the {@code TransactionAttribute},
 * the exposed name will be the {@code fully-qualified class name + "." + method name}
 * (by default).
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@code PlatformTransactionManager}
 * implementation will perform the actual transaction management, and a
 * {@code TransactionAttributeSource} is used for determining transaction definitions.
 *
 * <p>A transaction aspect is serializable if its {@code PlatformTransactionManager}
 * and {@code TransactionAttributeSource} are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author St茅phane Nicoll
 * @author Sam Brannen
 * @since 1.1
 * @see #setTransactionManager
 * @see #setTransactionAttributes
 * @see #setTransactionAttributeSource
 */

public abstract class TransactionAspectSupport  implements BeanFactoryAware, InitializingBean {


    /**
     * Key to use to store the default transaction manager.
     */
    private final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();

    // NOTE: This class must not implement Serializable because it serves as base
    // class for AspectJ aspects (which are not allowed to implement Serializable)!

    /**
     * Holder to support the {@code currentTransactionStatus()} method,
     * and to support communication between different cooperating advices
     * (e.g. before and after advice) if the aspect involves more than a
     * single method (as will be the case for around advice).
     */
    private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
            new NamedThreadLocal<TransactionInfo>("Current aspect-driven transaction");


    private final ConcurrentHashMap<Object, PlatformTransactionManager> transactionManagerCache =
            new ConcurrentHashMap<Object, PlatformTransactionManager>();

    /**
     * Subclasses can use this to return the current TransactionInfo.
     * Only subclasses that cannot handle all operations in one method,
     * such as an AspectJ aspect involving distinct before and after advice,
     * need to use this mechanism to get at the current TransactionInfo.
     * An around advice such as an AOP Alliance MethodInterceptor can hold a
     * reference to the TransactionInfo throughout the aspect method.
     * <p>A TransactionInfo will be returned even if no transaction was created.
     * The {@code TransactionInfo.hasTransaction()} method can be used to query this.
     * <p>To find out about specific transaction characteristics, consider using
     * TransactionSynchronizationManager's {@code isSynchronizationActive()}
     * and/or {@code isActualTransactionActive()} methods.
     * @return TransactionInfo bound to this thread, or {@code null} if none
     * @see TransactionInfo#hasTransaction()
     * @see org.springframework.transaction.support.TransactionSynchronizationManager#isSynchronizationActive()
     * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
     */
    protected static TransactionInfo currentTransactionInfo() throws NoTransactionException {
        return transactionInfoHolder.get();
    }

    /**
     * Return the transaction status of the current method invocation.
     * Mainly intended for code that wants to set the current transaction
     * rollback-only but not throw an application exception.
     * @throws NoTransactionException if the transaction info cannot be found,
     * because the method was invoked outside an AOP invocation context
     */
    public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
        TransactionInfo info = currentTransactionInfo();
        if (info == null || info.transactionStatus == null) {
            throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
        }
        return info.transactionStatus;
    }


    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Default transaction manager bean name.
     */
    private String transactionManagerBeanName;

    private TransactionAttributeSource transactionAttributeSource;

    private BeanFactory beanFactory;

    public void setTransactionManagerBeanName(String transactionManagerBeanName) {
        this.transactionManagerBeanName = transactionManagerBeanName;
    }

    protected final String getTransactionManagerBeanName() {
        return this.transactionManagerBeanName;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        if (transactionManager != null) {
            this.transactionManagerCache.put(DEFAULT_TRANSACTION_MANAGER_KEY, transactionManager);
        }
    }

    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
    }

    public void setTransactionAttributes(Properties transactionAttributes) {
        NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
        tas.setProperties(transactionAttributes);
        this.transactionAttributeSource = tas;
    }

    public void setTransactionAttributeSources(TransactionAttributeSource[] transactionAttributeSources) {
        this.transactionAttributeSource = new CompositeTransactionAttributeSource(transactionAttributeSources);
    }

    public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
        this.transactionAttributeSource = transactionAttributeSource;
    }

    public TransactionAttributeSource getTransactionAttributeSource() {
        return this.transactionAttributeSource;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    protected final BeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    /**
     * Check that required properties were set.
     */
    @Override
    public void afterPropertiesSet() {
        if (getTransactionManager() == null && this.beanFactory == null) {
            throw new IllegalStateException(
                    "Setting the property 'transactionManager' or running in a ListableBeanFactory is required");
        }
        if (this.transactionAttributeSource == null) {
            throw new IllegalStateException(
                    "Either 'transactionAttributeSource' or 'transactionAttributes' is required: " +
                            "If there are no transactional methods, then don't use a transaction aspect.");
        }
    }


    /**
     * General delegate for around-advice-based subclasses, delegating to several other template
     * methods on this class. Able to handle {@link CallbackPreferringPlatformTransactionManager}
     * as well as regular {@link PlatformTransactionManager} implementations.
     * @param method the Method being invoked
     * @param targetClass the target class that we're invoking the method on
     * @param invocation the callback to use for proceeding with the target invocation
     * @return the return value of the method, if any
     * @throws Throwable propagated from the target invocation
     */
    protected Object invokeWithinTransaction(Method method, Class<?> targetClass, final InvocationCallback invocation)
            throws Throwable {

        // If the transaction attribute is null, the method is non-transactional.
        // 获取事务的属性(传播行为,隔离级别等)
        final TransactionAttribute txAttr = getTransactionAttributeSource().getTransactionAttribute(method, targetClass);
        // TODO 获取事务管理器
        final PlatformTransactionManager tm = determineTransactionManager(txAttr);
        // 执行的方法
        // com.viewscenes.netsupervisor.service.impl.UserServiceImpl.insertUser
        final String joinpointIdentification = methodIdentification(method, targetClass);

        // 这里要区分不同的PlatformTransactionManager,因为它们的调用方式不同
        if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
            // Standard transaction demarcation with getTransaction and commit/rollback calls.
            // 创建事务,同时将创建事务过程得到的信息赋给TransactionInfo,让TransactionInfo保存事务的状态
            TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
            Object retVal = null;
            try {
                // This is an around advice: Invoke the next interceptor in the chain.
                // This will normally result in a target object being invoked.
                // 执行目标方法,使用调用沿着拦截器链进行,使最后目标对象的方法得到调用
                retVal = invocation.proceedWithInvocation();
            }
            catch (Throwable ex) {
                // target invocation exception
                // 如果整个事务过程出现异常,根据具体情况来决定回滚还是提交
                completeTransactionAfterThrowing(txInfo, ex);
                throw ex;
            }
            finally {
                // 这里把TransactionInfo设置oldTransactionInfo,表示该事务已经处理完
                cleanupTransactionInfo(txInfo);
            }
            // TODO 通过事务处理器来对事务进行提交
            commitTransactionAfterReturning(txInfo);
            return retVal;
        }

        else {
            // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
            try {
                Object result = ((CallbackPreferringPlatformTransactionManager) tm).execute(txAttr,
                        new TransactionCallback<Object>() {
                            @Override
                            public Object doInTransaction(TransactionStatus status) {
                                TransactionInfo txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
                                try {
                                    return invocation.proceedWithInvocation();
                                }
                                catch (Throwable ex) {
                                    if (txAttr.rollbackOn(ex)) {
                                        // A RuntimeException: will lead to a rollback.
                                        // A RuntimeException:会导致回滚
                                        if (ex instanceof RuntimeException) {
                                            throw (RuntimeException) ex;
                                        }
                                        else {
                                            throw new ThrowableHolderException(ex);
                                        }
                                    }
                                    else {
                                        // A normal return value: will lead to a commit.
                                        // 正常返回,事务提交
                                        return new ThrowableHolder(ex);
                                    }
                                }
                                finally {
                                    cleanupTransactionInfo(txInfo);
                                }
                            }
                        });

                // Check result: It might indicate a Throwable to rethrow.
                if (result instanceof ThrowableHolder) {
                    throw ((ThrowableHolder) result).getThrowable();
                }
                else {
                    return result;
                }
            }
            catch (ThrowableHolderException ex) {
                throw ex.getCause();
            }
        }
    }

    /**
     * Clear the cache.
     */
    protected void clearTransactionManagerCache() {
        this.transactionManagerCache.clear();
        this.beanFactory = null;
    }

    /**
     * Determine the specific transaction manager to use for the given transaction.
     */
    protected PlatformTransactionManager determineTransactionManager(TransactionAttribute txAttr) {
        // Do not attempt to lookup tx manager if no tx attributes are set
        if (txAttr == null || this.beanFactory == null) {
            return getTransactionManager();
        }
        String qualifier = txAttr.getQualifier();
        // 如果指定了Bean则取指定的PlatformTransactionManager类型的Bean
        if (StringUtils.hasText(qualifier)) {
            return determineQualifiedTransactionManager(qualifier);
        }
        //如果指定了Bean的名称,则根据bean名称获取对应的bean
        else if (StringUtils.hasText(this.transactionManagerBeanName)) {
            return determineQualifiedTransactionManager(this.transactionManagerBeanName);
        }
        // 默认取一个PlatformTransactionManager类型的Bean
        else {
            PlatformTransactionManager defaultTransactionManager = getTransactionManager();
            if (defaultTransactionManager == null) {
                defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
                this.transactionManagerCache.putIfAbsent(
                        DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
            }
            return defaultTransactionManager;
        }
    }

    private PlatformTransactionManager determineQualifiedTransactionManager(String qualifier) {
        PlatformTransactionManager txManager = this.transactionManagerCache.get(qualifier);
        if (txManager == null) {
            txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                    this.beanFactory, PlatformTransactionManager.class, qualifier);
            this.transactionManagerCache.putIfAbsent(qualifier, txManager);
        }
        return txManager;
    }

    protected String methodIdentification(Method method, Class<?> targetClass) {
        return (targetClass != null ? targetClass : method.getDeclaringClass()).getName() + "." + method.getName();
    }

    @SuppressWarnings("serial")
    protected TransactionInfo createTransactionIfNecessary(
            PlatformTransactionManager tm, TransactionAttribute txAttr, final String joinpointIdentification) {

        // If no name specified, apply method identification as transaction name.
        if (txAttr != null && txAttr.getName() == null) {
            txAttr = new DelegatingTransactionAttribute(txAttr) {
                @Override
                public String getName() {
                    return joinpointIdentification;
                }
            };
        }

        TransactionStatus status = null;
        if (txAttr != null) {
            if (tm != null) {
                // TODO 创建事务
                status = tm.getTransaction(txAttr);
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
                            "] because no transaction manager has been configured");
                }
            }
        }
        // TODO 封装事务对象,将事务绑定到当前线程,即ThreadLocal。
        return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
    }

    protected TransactionInfo prepareTransactionInfo(PlatformTransactionManager tm,
                                                     TransactionAttribute txAttr, String joinpointIdentification, TransactionStatus status) {
        // 将事务管理器,事务属性和方法封装成TransactionInfo对象
        TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
        if (txAttr != null) {
            // We need a transaction for this method
            if (logger.isTraceEnabled()) {
                logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
            }
            // The transaction manager will flag an error if an incompatible tx already exists
            // 设置事务的状态
            txInfo.newTransactionStatus(status);
        }
        else {
            // The TransactionInfo.hasTransaction() method will return
            // false. We created it only to preserve the integrity of
            // the ThreadLocal stack maintained in this class.
            if (logger.isTraceEnabled())
                logger.trace("Don't need to create transaction for [" + joinpointIdentification +
                        "]: This method isn't transactional.");
        }

        // We always bind the TransactionInfo to the thread, even if we didn't create
        // a new transaction here. This guarantees that the TransactionInfo stack
        // will be managed correctly even if no transaction was created by this aspect.
        // 将事务绑定到当前线程,即ThreadLocal。
        // 因为在commit的时候,会判断当前线程是否有事务存在,否则不会提交
        txInfo.bindToThread();
        return txInfo;
    }

    protected void commitTransactionAfterReturning(TransactionInfo txInfo) {
        if (txInfo != null && txInfo.hasTransaction()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
            }
            txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
        }
    }

    protected void completeTransactionAfterThrowing(TransactionInfo txInfo, Throwable ex) {
        if (txInfo != null && txInfo.hasTransaction()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
                        "] after exception: " + ex);
            }
            if (txInfo.transactionAttribute.rollbackOn(ex)) {
                try {
                    txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
                }
                catch (TransactionSystemException ex2) {
                    logger.error("Application exception overridden by rollback exception", ex);
                    ex2.initApplicationException(ex);
                    throw ex2;
                }
                catch (RuntimeException ex2) {
                    logger.error("Application exception overridden by rollback exception", ex);
                    throw ex2;
                }
                catch (Error err) {
                    logger.error("Application exception overridden by rollback error", ex);
                    throw err;
                }
            }
            else {
                // We don't roll back on this exception.
                // Will still roll back if TransactionStatus.isRollbackOnly() is true.
                try {
                    txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
                }
                catch (TransactionSystemException ex2) {
                    logger.error("Application exception overridden by commit exception", ex);
                    ex2.initApplicationException(ex);
                    throw ex2;
                }
                catch (RuntimeException ex2) {
                    logger.error("Application exception overridden by commit exception", ex);
                    throw ex2;
                }
                catch (Error err) {
                    logger.error("Application exception overridden by commit error", ex);
                    throw err;
                }
            }
        }
    }

    protected void cleanupTransactionInfo(TransactionInfo txInfo) {
        if (txInfo != null) {
            txInfo.restoreThreadLocalStatus();
        }
    }


    /**
     * Opaque object used to hold Transaction information. Subclasses
     * must pass it back to methods on this class, but not see its internals.
     */
    protected final class TransactionInfo {

        private final PlatformTransactionManager transactionManager;

        private final TransactionAttribute transactionAttribute;

        private final String joinpointIdentification;

        private TransactionStatus transactionStatus;

        private TransactionInfo oldTransactionInfo;

        public TransactionInfo(PlatformTransactionManager transactionManager,
                               TransactionAttribute transactionAttribute, String joinpointIdentification) {
            this.transactionManager = transactionManager;
            this.transactionAttribute = transactionAttribute;
            this.joinpointIdentification = joinpointIdentification;
        }

        public PlatformTransactionManager getTransactionManager() {
            return this.transactionManager;
        }

        public TransactionAttribute getTransactionAttribute() {
            return this.transactionAttribute;
        }

        /**
         * Return a String representation of this joinpoint (usually a Method call)
         * for use in logging.
         */
        public String getJoinpointIdentification() {
            return this.joinpointIdentification;
        }

        public void newTransactionStatus(TransactionStatus status) {
            this.transactionStatus = status;
        }

        public TransactionStatus getTransactionStatus() {
            return this.transactionStatus;
        }

        /**
         * Return whether a transaction was created by this aspect,
         * or whether we just have a placeholder to keep ThreadLocal stack integrity.
         */
        public boolean hasTransaction() {
            return (this.transactionStatus != null);
        }

        private void bindToThread() {
            // Expose current TransactionStatus, preserving any existing TransactionStatus
            // for restoration after this transaction is complete.
            this.oldTransactionInfo = transactionInfoHolder.get();
            transactionInfoHolder.set(this);
        }

        private void restoreThreadLocalStatus() {
            // Use stack to restore old transaction TransactionInfo.
            // Will be null if none was set.
            transactionInfoHolder.set(this.oldTransactionInfo);
        }

        @Override
        public String toString() {
            return this.transactionAttribute.toString();
        }
    }


    /**
     * Simple callback interface for proceeding with the target invocation.
     * Concrete interceptors/aspects adapt this to their invocation mechanism.
     */
    protected interface InvocationCallback {

        Object proceedWithInvocation() throws Throwable;
    }


    /**
     * Internal holder class for a Throwable, used as a return value
     * from a TransactionCallback (to be subsequently unwrapped again).
     */
    private static class ThrowableHolder {

        private final Throwable throwable;

        public ThrowableHolder(Throwable throwable) {
            this.throwable = throwable;
        }

        public final Throwable getThrowable() {
            return this.throwable;
        }
    }


    /**
     * Internal holder class for a Throwable, used as a RuntimeException to be
     * thrown from a TransactionCallback (and subsequently unwrapped again).
     */
    @SuppressWarnings("serial")
    private static class ThrowableHolderException extends RuntimeException {

        public ThrowableHolderException(Throwable throwable) {
            super(throwable);
        }

        @Override
        public String toString() {
            return getCause().toString();
        }
    }


}
