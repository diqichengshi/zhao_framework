package org.springframework.util;

public interface StringValueResolver {

    /**
     * Resolve the given String value, for example parsing placeholders.
     * @param strVal the original String value
     * @return the resolved String value
     */
    String resolveStringValue(String strVal);

}