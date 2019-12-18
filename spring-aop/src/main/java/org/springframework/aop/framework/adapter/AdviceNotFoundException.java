package org.springframework.aop.framework.adapter;

import org.springframework.aop.Advisor;

public class AdviceNotFoundException extends IllegalArgumentException {

	private static final long serialVersionUID = -7651798875327971017L;

	public AdviceNotFoundException(Advisor advisor) {
		super("Advice not found on [" + advisor + "]");
	}

	public AdviceNotFoundException(String message) {
		super(message);
	}

}
