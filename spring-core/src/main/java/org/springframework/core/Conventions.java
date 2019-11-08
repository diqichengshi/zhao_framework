package org.springframework.core;

import org.springframework.util.Assert;

public abstract class Conventions {
    /**
     * Return an attribute name qualified by the supplied enclosing {@link Class}. For example,
     * the attribute name '{@code foo}' qualified by {@link Class} '{@code com.myapp.SomeClass}'
     * would be '{@code com.myapp.SomeClass.foo}'
     */
    public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
        Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
        Assert.notNull(attributeName, "'attributeName' must not be null");
        return enclosingClass.getName() + "." + attributeName;
    }
}
