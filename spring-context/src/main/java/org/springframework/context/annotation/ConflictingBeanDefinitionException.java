package org.springframework.context.annotation;

public class ConflictingBeanDefinitionException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public ConflictingBeanDefinitionException(String message) {
        super(message);
    }

}
