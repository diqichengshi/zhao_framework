package org.springframework.transaction.interceptor;

import org.springframework.beans.factory.BeanFactoryAware;
import java.util.Properties;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Proxy factory bean for simplified declarative transaction handling.
 * This is a convenient alternative to a standard AOP
 * {@link org.springframework.aop.framework.ProxyFactoryBean}
 * with a separate {@link TransactionInterceptor} definition.
 *
 * <p><strong>HISTORICAL NOTE:</strong> This class was originally designed to cover the
 * typical case of declarative transaction demarcation: namely, wrapping a singleton
 * target object with a transactional proxy, proxying all the interfaces that the target
 * implements. However, in Spring versions 2.0 and beyond, the functionality provided here
 * is superseded by the more convenient {@code tx:} XML namespace. See the <a
 * href="http://bit.ly/qUwvwz">declarative transaction management</a> section of the
 * Spring reference documentation to understand the modern options for managing
 * transactions in Spring applications. For these reasons, <strong>users should favor of
 * the {@code tx:} XML namespace as well as
 * the @{@link org.springframework.transaction.annotation.Transactional Transactional}
 * and @{@link org.springframework.transaction.annotation.EnableTransactionManagement
 * EnableTransactionManagement} annotations.</strong>
 *
 * <p>There are three main properties that need to be specified:
 * <ul>
 * <li>"transactionManager": the {@link PlatformTransactionManager} implementation to use
 * (for example, a {@link org.springframework.transaction.jta.JtaTransactionManager} instance)
 * <li>"target": the target object that a transactional proxy should be created for
 * <li>"transactionAttributes": the transaction attributes (for example, propagation
 * behavior and "readOnly" flag) per target method name (or method name pattern)
 * </ul>
 *
 * <p>If the "transactionManager" property is not set explicitly and this {@link FactoryBean}
 * is running in a {@link ListableBeanFactory}, a single matching bean of type
 * {@link PlatformTransactionManager} will be fetched from the {@link BeanFactory}.
 *
 * <p>In contrast to {@link TransactionInterceptor}, the transaction attributes are
 * specified as properties, with method names as keys and transaction attribute
 * descriptors as values. Method names are always applied to the target class.
 *
 * <p>Internally, a {@link TransactionInterceptor} instance is used, but the user of this
 * class does not have to care. Optionally, a method pointcut can be specified
 * to cause conditional invocation of the underlying {@link TransactionInterceptor}.
 *
 * <p>The "preInterceptors" and "postInterceptors" properties can be set to add
 * additional interceptors to the mix, like
 * {@link org.springframework.aop.interceptor.PerformanceMonitorInterceptor}.
 *
 * <p><b>HINT:</b> This class is often used with parent / child bean definitions.
 * Typically, you will define the transaction manager and default transaction
 * attributes (for method name patterns) in an abstract parent bean definition,
 * deriving concrete child bean definitions for specific target objects.
 * This reduces the per-bean definition effort to a minimum.
 *
 * <pre code="class">
 * {@code
 * <bean id="baseTransactionProxy" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean"
 *     abstract="true">
 *   <property name="transactionManager" ref="transactionManager"/>
 *   <property name="transactionAttributes">
 *     <props>
 *       <prop key="insert*">PROPAGATION_REQUIRED</prop>
 *       <prop key="update*">PROPAGATION_REQUIRED</prop>
 *       <prop key="*">PROPAGATION_REQUIRED,readOnly</prop>
 *     </props>
 *   </property>
 * </bean>
 *
 * <bean id="myProxy" parent="baseTransactionProxy">
 *   <property name="target" ref="myTarget"/>
 * </bean>
 *
 * <bean id="yourProxy" parent="baseTransactionProxy">
 *   <property name="target" ref="yourTarget"/>
 * </bean>}</pre>
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @author Rod Johnson
 * @author Chris Beams
 * @since 21.08.2003
 * @see #setTransactionManager
 * @see #setTarget
 * @see #setTransactionAttributes
 * @see TransactionInterceptor
 * @see org.springframework.aop.framework.ProxyFactoryBean
 */
public class TransactionProxyFactoryBean extends AbstractSingletonProxyFactoryBean
        implements BeanFactoryAware {
    // 这个拦截器就是发挥来AOP作用,其中封装了对事务的操作
    private final TransactionInterceptor transactionInterceptor = new TransactionInterceptor();

    private Pointcut pointcut;

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionInterceptor.setTransactionManager(transactionManager);
    }

    // 通过依赖注入将配置事务属性
    public void setTransactionAttributes(Properties transactionAttributes) {
        this.transactionInterceptor.setTransactionAttributes(transactionAttributes);
    }

    public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
        this.transactionInterceptor.setTransactionAttributeSource(transactionAttributeSource);
    }

    public void setPointcut(Pointcut pointcut) {
        this.pointcut = pointcut;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.transactionInterceptor.setBeanFactory(beanFactory);
    }


    /**
     * Creates an advisor for this FactoryBean's TransactionInterceptor.
     * 创建AOP的通知器
     */
    @Override
    protected Object createMainInterceptor() {
        // 事务处理完成AOP配置
        this.transactionInterceptor.afterPropertiesSet();
        if (this.pointcut != null) {
            return new DefaultPointcutAdvisor(this.pointcut, this.transactionInterceptor);
        }
        else {
            // Rely on default pointcut.
            return new TransactionAttributeSourceAdvisor(this.transactionInterceptor);
        }
    }

    /**
     * As of 4.2, this method adds {@link TransactionalProxy} to the set of
     * proxy interfaces in order to avoid re-processing of transaction metadata.
     */
    @Override
    protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
        proxyFactory.addInterface(TransactionalProxy.class);
    }

}
