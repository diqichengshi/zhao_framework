package org.springframework.aop.aspectj;

import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.TypePatternMatcher;
import org.springframework.aop.ClassFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class TypePatternClassFilter implements ClassFilter {
    @SuppressWarnings({"unused"})
    private String typePattern;
    private TypePatternMatcher aspectJTypePatternMatcher;

    public TypePatternClassFilter() {
    }

    public TypePatternClassFilter(String typePattern) {
        setTypePattern(typePattern);
    }

    public void setTypePattern(String typePattern) {
        Assert.notNull(typePattern);
        this.typePattern = typePattern;
        this.aspectJTypePatternMatcher =
                PointcutParser.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution().
                        parseTypePattern(replaceBooleanOperators(typePattern));
    }

    @Override
    public boolean matches(Class<?> clazz) {
        if (this.aspectJTypePatternMatcher == null) {
            throw new IllegalStateException("No 'typePattern' has been set via ctor/setter.");
        }
        return this.aspectJTypePatternMatcher.matches(clazz);
    }

    /**
     * If a type pattern has been specified in XML, the user cannot
     * write {@code and} as "&&" (though &amp;&amp; will work).
     * We also allow {@code and} between two sub-expressions.
     * <p>This method converts back to {@code &&} for the AspectJ pointcut parser.
     */
    private String replaceBooleanOperators(String pcExpr) {
        pcExpr = StringUtils.replace(pcExpr," and "," && ");
        pcExpr = StringUtils.replace(pcExpr, " or ", " || ");
        pcExpr = StringUtils.replace(pcExpr, " not ", " ! ");
        return pcExpr;
    }

}
