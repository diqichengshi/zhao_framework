package org.springframework.dao;
/**
 * Normal superclass when we can't distinguish anything more specific
 * than "something went wrong with the underlying resource": for example,
 * a SQLException from JDBC we can't pinpoint more precisely.
 *
 * @author Rod Johnson
 */
public class UncategorizedDataAccessException  extends NonTransientDataAccessException {

    /**
     * Constructor for UncategorizedDataAccessException.
     * @param msg the detail message
     * @param cause the exception thrown by underlying data access API
     */
    public UncategorizedDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
