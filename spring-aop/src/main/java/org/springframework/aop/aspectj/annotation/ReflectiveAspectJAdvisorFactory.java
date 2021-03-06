package org.springframework.aop.aspectj.annotation;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.*;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.*;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConvertingComparator;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.CompoundComparator;
import org.springframework.util.comparator.InstanceComparator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory {
    protected final Log logger = LogFactory.getLog(getClass());
    private static final Comparator<Method> METHOD_COMPARATOR;

    static {
        CompoundComparator<Method> comparator = new CompoundComparator<Method>();
        comparator.addComparator(new ConvertingComparator<Method, Annotation>(
                new InstanceComparator<Annotation>(
                        Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
                new Converter<Method, Annotation>() {
                    @Override
                    public Annotation convert(Method method) {
                        AspectJAnnotation<?> annotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
                        return annotation == null ? null : annotation.getAnnotation();
                    }
                }));
        comparator.addComparator(new ConvertingComparator<Method, String>(
                new Converter<Method, String>() {
                    @Override
                    public String convert(Method method) {
                        return method.getName();
                    }
                }));
        METHOD_COMPARATOR = comparator;
    }

    @Override
    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory maaif) {
        final Class<?> aspectClass = maaif.getAspectMetadata().getAspectClass();
        final String aspectName = maaif.getAspectMetadata().getAspectName();
        validate(aspectClass);

        // We need to wrap the MetadataAwareAspectInstanceFactory with a decorator
        // so that it will only instantiate once.
        final MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
                new LazySingletonAspectInstanceFactoryDecorator(maaif);

        final List<Advisor> advisors = new LinkedList<Advisor>();
        // 获取增强方法
        for (Method method : getAdvisorMethods(aspectClass)) {
            // TODO 获取增强器
            Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        // If it's a per target aspect, emit the dummy instantiating aspect.
        if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
            Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
            // 如果需要增强并且aspectj的初始化模式配置为延迟初始化则需要在增强器集合首位加入同步实例化增强器
            advisors.add(0, instantiationAdvisor);
        }

        // Find introduction fields.
        for (Field field : aspectClass.getDeclaredFields()) {
            // declared标签的解析
            Advisor advisor = getDeclareParentsAdvisor(field);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        return advisors;
    }

    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        final List<Method> methods = new LinkedList<Method>();
        ReflectionUtils.doWithMethods(aspectClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException {
                // Exclude pointcuts
                if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
                    methods.add(method);
                }
            }
        });
        Collections.sort(methods, METHOD_COMPARATOR);
        return methods;
    }

    /**
     * Build a {@link org.springframework.aop.aspectj.DeclareParentsAdvisor}
     * for the given introduction field.
     * <p>Resulting Advisors will need to be evaluated for targets.
     *
     * @param introductionField the field to introspect
     * @return {@code null} if not an Advisor
     */
    private Advisor getDeclareParentsAdvisor(Field introductionField) {
        DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
        if (declareParents == null) {
            // Not an introduction field
            return null;
        }

        if (DeclareParents.class == declareParents.defaultImpl()) {
            // This is what comes back if it wasn't set. This seems bizarre...
            throw new IllegalStateException("defaultImpl must be set on DeclareParents");
        }

        return new DeclareParentsAdvisor(
                introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
    }

    /**
     * 获取增强器
     */
    @Override
    public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aif,
                              int declarationOrderInAspect, String aspectName) {
        validate(aif.getAspectMetadata().getAspectClass());

        // TODO 切点信息的获取
        AspectJExpressionPointcut ajexp =
                getPointcut(candidateAdviceMethod, aif.getAspectMetadata().getAspectClass());
        if (ajexp == null) {
            return null;
        }
        // TODO 根据切点信息生成增强器
        // 所有的增强都由Advisor的实现类InstantiationModelAwarePointcutAdvisorImpl统一封装的
        return new InstantiationModelAwarePointcutAdvisorImpl(
                this, ajexp, aif, candidateAdviceMethod, declarationOrderInAspect, aspectName);
    }

    /**
     * 根据注解类型的不同封装对应的增强器
     */
    @Override
    public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut ajexp,
                            MetadataAwareAspectInstanceFactory aif, int declarationOrderInAspect, String aspectName) {
        Class<?> candidateAspectClass = aif.getAspectMetadata().getAspectClass();
        validate(candidateAspectClass);

        AspectJAnnotation<?> aspectJAnnotation =
                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (aspectJAnnotation == null) {
            return null;
        }

        // If we get here, we know we have an AspectJ method.
        // Check that it's an AspectJ-annotated class
        if (!isAspect(candidateAspectClass)) {
            throw new AopConfigException("Advice must be declared inside an aspect type: " +
                    "Offending method '" + candidateAdviceMethod + "' in class [" +
                    candidateAspectClass.getName() + "]");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Found AspectJ method: " + candidateAdviceMethod);
        }

        AbstractAspectJAdvice springAdvice;
        // 根据注解类型的不同封装不同的增强器
        switch (aspectJAnnotation.getAnnotationType()) {
            case AtBefore:
                springAdvice = new AspectJMethodBeforeAdvice(candidateAdviceMethod, ajexp, aif);
                break;
            case AtAfter:
                springAdvice = new AspectJAfterAdvice(candidateAdviceMethod, ajexp, aif);
                break;
            case AtAfterReturning:
                springAdvice = new AspectJAfterReturningAdvice(candidateAdviceMethod, ajexp, aif);
                AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
                if (StringUtils.hasText(afterReturningAnnotation.returning())) {
                    springAdvice.setReturningName(afterReturningAnnotation.returning());
                }
                break;
            case AtAfterThrowing:
                springAdvice = new AspectJAfterThrowingAdvice(candidateAdviceMethod, ajexp, aif);
                AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
                if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
                    springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
                }
                break;
            case AtAround:
                springAdvice = new AspectJAroundAdvice(candidateAdviceMethod, ajexp, aif);
                break;
            case AtPointcut:
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
                }
                return null;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported advice type on method " + candidateAdviceMethod);
        }

        // Now to configure the advice...
        springAdvice.setAspectName(aspectName);
        springAdvice.setDeclarationOrder(declarationOrderInAspect);
        String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
        if (argNames != null) {
            springAdvice.setArgumentNamesFromStringArray(argNames);
        }
        springAdvice.calculateArgumentBindings();
        return springAdvice;
    }

    private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        // 找到方法上的注解
        AspectJAnnotation<?> aspectJAnnotation =
                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (aspectJAnnotation == null) {
            return null;
        }
        // 使用AspectJExpressionPointcut实例封装获取的信息
        AspectJExpressionPointcut ajexp =
                new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
        // 提取注解上的表达式,例如:execution(org.springframework.tests.aop.say(..))
        ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
        return ajexp;
    }

    /**
     * Synthetic advisor that instantiates the aspect.
     * Triggered by per-clause pointcut on non-singleton aspect.
     * The advice has no effect.
     */
    @SuppressWarnings("serial")
    protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {

        public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
            super(aif.getAspectMetadata().getPerClausePointcut(), new MethodBeforeAdvice() {
                @Override
                public void before(Method method, Object[] args, Object target) {
                    // Simply instantiate the aspect
                    aif.getAspectInstance();
                }
            });
        }
    }
}
