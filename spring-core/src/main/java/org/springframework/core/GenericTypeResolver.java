package org.springframework.core;

import org.springframework.util.Assert;

import java.lang.reflect.Method;

public class GenericTypeResolver {

    /**
     * Determine the target type for the given generic parameter type.
     *
     * @param methodParam the method parameter specification
     * @param clazz       the class to resolve type variables against
     * @return the corresponding generic parameter or return type
     */
    public static Class<?> resolveParameterType(MethodParameter methodParam, Class<?> clazz) {
        Assert.notNull(methodParam, "MethodParameter must not be null");
        Assert.notNull(clazz, "Class must not be null");
        methodParam.setContainingClass(clazz);
        methodParam.setParameterType(ResolvableType.forMethodParameter(methodParam).resolve());
        return methodParam.getParameterType();
    }

    /**
     * Determine the target type for the generic return type of the given method,
     * where formal type variables are declared on the given class.
     *
     * @param method the method to introspect
     * @param clazz  the class to resolve type variables against
     * @return the corresponding generic parameter or return type
     * @see #resolveReturnTypeForGenericMethod
     */
    public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(clazz, "Class must not be null");
        return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
    }

}
