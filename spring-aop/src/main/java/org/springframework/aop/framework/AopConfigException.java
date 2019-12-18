package org.springframework.aop.framework;

import org.springframework.core.NestedRuntimeException;

public class AopConfigException extends NestedRuntimeException {

    private static final long serialVersionUID = -1963594012763536970L;

    /**
     * Constructor for AopConfigException.
     * 
     * @param msg the detail message
     */
    public AopConfigException(String msg) {
        super(msg);
    }

    /**
     * Constructor for AopConfigException.
     * @param msg the detail message
     * @param cause the root cause
     */
    public AopConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

