package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.TypePatternClassFilter;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.ComposablePointcut;

/**
 * Metadata for an AspectJ aspect class, with an additional Spring AOP pointcut
 * for the per clause.
 *
 * <p>Uses AspectJ 5 AJType reflection API, so is only supported on Java 5.
 * Enables us to work with different AspectJ instantiation models such as
 * "singleton", "pertarget" and "perthis".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.aop.aspectj.AspectJExpressionPointcut
 */

public class AspectMetadata {
    /**
     * AspectJ reflection information (AspectJ 5 / Java 5 specific).
     */
    private final AjType<?> ajType;

    /**
     * Spring AOP pointcut corresponding to the per clause of the
     * aspect. Will be the Pointcut.TRUE canonical instance in the
     * case of a singleton, otherwise an AspectJExpressionPointcut.
     */
    private final Pointcut perClausePointcut;

    /**
     * The name of this aspect as defined to Spring (the bean name) -
     * allows us to determine if two pieces of advice come from the
     * same aspect and hence their relative precedence.
     */
    private String aspectName;


    /**
     * Create a new AspectMetadata instance for the given aspect class.
     * @param aspectClass the aspect class
     * @param aspectName the name of the aspect
     */
    public AspectMetadata(Class<?> aspectClass, String aspectName) {
        this.aspectName = aspectName;

        Class<?> currClass = aspectClass;
        AjType<?> ajType = null;
        while (currClass != Object.class) {
            AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
            if (ajTypeToCheck.isAspect()) {
                ajType = ajTypeToCheck;
                break;
            }
            currClass = currClass.getSuperclass();
        }
        if (ajType == null) {
            throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
        }
        this.ajType = ajType;
        if (this.ajType.getDeclarePrecedence().length > 0) {
            throw new IllegalArgumentException("DeclarePrecendence not presently supported in Spring AOP");
        }

        switch (this.ajType.getPerClause().getKind()) {
            case SINGLETON :
                this.perClausePointcut = Pointcut.TRUE;
                return;
           /* case PERTARGET : case PERTHIS :
                AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
                ajexp.setLocation("@Aspect annotation on " + aspectClass.getName());
                ajexp.setExpression(findPerClause(aspectClass));
                this.perClausePointcut = ajexp;
                return;*/
            case PERTYPEWITHIN :
                // Works with a type pattern
                this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
                return;
            default :
                throw new AopConfigException(
                        "PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
        }
    }

    /**
     * Extract contents from String of form {@code pertarget(contents)}.
     */
    private String findPerClause(Class<?> aspectClass) {
        // bother to make it elegant. Or efficient. Or robust :-)
        String str = aspectClass.getAnnotation(Aspect.class).value();
        str = str.substring(str.indexOf("(") + 1);
        str = str.substring(0, str.length() - 1);
        return str;
    }


    /**
     * Return AspectJ reflection information.
     */
    public AjType<?> getAjType() {
        return this.ajType;
    }

    /**
     * Return the aspect class.
     */
    public Class<?> getAspectClass() {
        return this.ajType.getJavaClass();
    }

    /**
     * Return the aspect class.
     */
    public String getAspectName() {
        return this.aspectName;
    }

    /**
     * Return a Spring pointcut expression for a singleton aspect.
     * (e.g. {@code Pointcut.TRUE} if it's a singleton).
     */
    public Pointcut getPerClausePointcut() {
        return this.perClausePointcut;
    }

    /**
     * Return whether the aspect is defined as "perthis" or "pertarget".
     */
    public boolean isPerThisOrPerTarget() {
        PerClauseKind kind = getAjType().getPerClause().getKind();
        return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
    }

    /**
     * Return whether the aspect is defined as "pertypewithin".
     */
    public boolean isPerTypeWithin() {
        PerClauseKind kind = getAjType().getPerClause().getKind();
        return (kind == PerClauseKind.PERTYPEWITHIN);
    }

    /**
     * Return whether the aspect needs to be lazily instantiated.
     */
    public boolean isLazilyInstantiated() {
        return (isPerThisOrPerTarget() || isPerTypeWithin());
    }

}
