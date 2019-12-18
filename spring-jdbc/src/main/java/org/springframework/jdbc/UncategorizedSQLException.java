package org.springframework.jdbc;

import org.springframework.dao.UncategorizedDataAccessException;

import java.sql.SQLException;

public class UncategorizedSQLException extends UncategorizedDataAccessException {

	private static final long serialVersionUID = 1L;
	/** SQL that led to the problem */
    private final String sql;


    /**
     * Constructor for UncategorizedSQLException.
     * @param task name of current task
     * @param sql the offending SQL statement
     * @param ex the root cause
     */
    public UncategorizedSQLException(String task, String sql, SQLException ex) {
        super(task + "; uncategorized SQLException for SQL [" + sql + "]; SQL state [" +
                ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " + ex.getMessage(), ex);
        this.sql = sql;
    }


    /**
     * Return the underlying SQLException.
     */
    public SQLException getSQLException() {
        return (SQLException) getCause();
    }

    /**
     * Return the SQL that led to the problem.
     */
    public String getSql() {
        return this.sql;
    }

}

