package org.springframework.jdbc.core;

import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * Interface specifying a basic set of JDBC operations.
 * Implemented by {@link JdbcTemplate}. Not often used directly, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Alternatively, the standard JDBC infrastructure can be mocked.
 * However, mocking this interface constitutes significantly less work.
 * As an alternative to a mock objects approach to testing data access code,
 * consider the powerful integration testing support provided in the
 * {@code org.springframework.test} package, shipped in
 * {@code spring-test.jar}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JdbcTemplate
 */
public interface JdbcOperations {

    //-------------------------------------------------------------------------
    // Methods dealing with static SQL (java.sql.Statement)
    //-------------------------------------------------------------------------

    /**
     * Execute a JDBC data access operation, implemented as callback action
     * working on a JDBC Statement. This allows for implementing arbitrary data
     * access operations on a single Statement, within Spring's managed JDBC
     * environment: that is, participating in Spring-managed transactions and
     * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
     * <p>The callback action can return a result object, for example a
     * domain object or a collection of domain objects.
     *
     * @param action callback object that specifies the action
     * @return a result object returned by the action, or {@code null}
     * @throws DataAccessException if there is any problem
     */
    <T> T execute(StatementCallback<T> action) throws DataAccessException;

    /**
     * Execute a query given static SQL, reading the ResultSet with a
     * ResultSetExtractor.
     * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
     * execute a static query with a PreparedStatement, use the overloaded
     * {@code query} method with {@code null} as argument array.
     * @param sql SQL query to execute
     * @param rse object that will extract all rows of results
     * @return an arbitrary result object, as returned by the ResultSetExtractor
     * @throws DataAccessException if there is any problem executing the query
     * @see #query(String, Object[], ResultSetExtractor)
     */
    <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;
    /**
     * Execute a query given static SQL, mapping each row to a Java object
     * via a RowMapper.
     * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
     * execute a static query with a PreparedStatement, use the overloaded
     * {@code query} method with {@code null} as argument array.
     *
     * @param sql       SQL query to execute
     * @param rowMapper object that will map one object per row
     * @return the result List, containing mapped objects
     * @throws DataAccessException if there is any problem executing the query
     * @see #query(String, Object[], RowMapper)
     */
    <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * Issue a single SQL update operation (such as an insert, update or delete statement).
     *
     * @param sql static SQL to execute
     * @return the number of rows affected
     * @throws DataAccessException if there is any problem.
     */
    int update(String sql) throws DataAccessException;

    //-------------------------------------------------------------------------
    // Methods dealing with prepared statements
    //-------------------------------------------------------------------------

    /**
     * Execute a JDBC data access operation, implemented as callback action
     * working on a JDBC PreparedStatement. This allows for implementing arbitrary
     * data access operations on a single Statement, within Spring's managed
     * JDBC environment: that is, participating in Spring-managed transactions
     * and converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
     * <p>The callback action can return a result object, for example a
     * domain object or a collection of domain objects.
     * @param psc object that can create a PreparedStatement given a Connection
     * @param action callback object that specifies the action
     * @return a result object returned by the action, or {@code null}
     * @throws DataAccessException if there is any problem
     */
    <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException;

    /**
     * Issue an update statement using a PreparedStatementSetter to set bind parameters,
     * with given SQL. Simpler than using a PreparedStatementCreator as this method
     * will create the PreparedStatement: The PreparedStatementSetter just needs to
     * set parameters.
     * @param sql SQL containing bind parameters
     * @param pss helper that sets bind parameters. If this is {@code null}
     * we run an update with static SQL.
     * @return the number of rows affected
     * @throws DataAccessException if there is any problem issuing the update
     */
    int update(String sql, PreparedStatementSetter pss) throws DataAccessException;
    /**
     * Issue a single SQL update operation (such as an insert, update or delete statement)
     * via a prepared statement, binding the given arguments.
     * @param sql SQL containing bind parameters
     * @param args arguments to bind to the query
     * (leaving it to the PreparedStatement to guess the corresponding SQL type);
     * may also contain {@link SqlParameterValue} objects which indicate not
     * only the argument value but also the SQL type and optionally the scale
     * @return the number of rows affected
     * @throws DataAccessException if there is any problem issuing the update
     */
    int update(String sql, Object... args) throws DataAccessException;
}
