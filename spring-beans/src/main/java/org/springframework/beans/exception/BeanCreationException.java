package org.springframework.beans.exception;

import org.springframework.util.ObjectUtils;

import java.util.LinkedList;
import java.util.List;

public class BeanCreationException extends RuntimeException {
    private List<Throwable> relatedCauses;

    /**
     * Create a new BeansException with the specified message.
     *
     * @param msg the detail message
     */
    public BeanCreationException(String msg) {
        super(msg);
    }

    /**
     * Create a new BeansException with the specified message
     * and root cause.
     *
     * @param msg   the detail message
     * @param cause the root cause
     */
    public BeanCreationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public void addRelatedCause(Throwable ex) {
        if (this.relatedCauses == null) {
            this.relatedCauses = new LinkedList<Throwable>();
        }
        this.relatedCauses.add(ex);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeanCreationException)) {
            return false;
        }
        BeanCreationException otherBe = (BeanCreationException) other;
        return (getMessage().equals(otherBe.getMessage()) &&
                ObjectUtils.nullSafeEquals(getCause(), otherBe.getCause()));
    }

    @Override
    public int hashCode() {
        return getMessage().hashCode();
    }
}
