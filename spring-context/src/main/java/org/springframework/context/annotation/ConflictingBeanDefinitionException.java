package org.springframework.context.annotation;

public class ConflictingBeanDefinitionException extends IllegalStateException {

    public ConflictingBeanDefinitionException(String message) {
        super(message);
    }

}
