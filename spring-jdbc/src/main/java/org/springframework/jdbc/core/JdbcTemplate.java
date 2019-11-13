package org.springframework.jdbc.core;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

/**
 * <b>This is the central class in the JDBC core package.</b>
 * It simplifies the use of JDBC and helps to avoid common errors.
 * It executes core JDBC workflow, leaving application code to provide SQL
 * and extract results. This class executes SQL queries or updates, initiating
 * iteration over ResultSets and catching JDBC exceptions and translating
 * them to the generic, more informative exception hierarchy defined in the
 * {@code org.springframework.dao} package.
 *
 * <p>Code using this class need only implement callback interfaces, giving
 * them a clearly defined contract. The {@link PreparedStatementCreator} callback
 * interface creates a prepared statement given a Connection, providing SQL and
 * any necessary parameters. The {@link ResultSetExtractor} interface extracts
 * values from a ResultSet. See also {@link PreparedStatementSetter} and
 * {@link RowMapper} for two popular alternative callback interfaces.
 *
 * <p>Can be used within a service implementation via direct instantiation
 * with a DataSource reference, or get prepared in an application context
 * and given to services as bean reference. Note: The DataSource should
 * always be configured as a bean in the application context, in the first case
 * given to the service directly, in the second case to the prepared template.
 *
 * <p>Because this class is parameterizable by the callback interfaces and
 * the {@link org.springframework.jdbc.support.SQLExceptionTranslator}
 * interface, there should be no need to subclass it.
 *
 * <p>All SQL operations performed by this class are logged at debug level,
 * using "org.springframework.jdbc.core.JdbcTemplate" as log category.
 *
 * <p><b>NOTE: An instance of this class is thread-safe once configured.</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since May 3, 2001
 * @see PreparedStatementCreator
 * @see PreparedStatementSetter
 * @see CallableStatementCreator
 * @see PreparedStatementCallback
 * @see CallableStatementCallback
 * @see ResultSetExtractor
 * @see RowCallbackHandler
 * @see RowMapper
 * @see org.springframework.jdbc.support.SQLExceptionTranslator
 */
public class JdbcTemplate extends JdbcAccessor implements JdbcOperations {

    private NativeJdbcExtractor nativeJdbcExtractor;
    private boolean ignoreWarnings = true;
    private int fetchSize = -1;
    private int maxRows = -1;
    private int queryTimeout = -1;

    public JdbcTemplate() {
    }

    public JdbcTemplate(DataSource dataSource) {
        setDataSource(dataSource);
        afterPropertiesSet();
    }

    public void setNativeJdbcExtractor(NativeJdbcExtractor extractor) {
        this.nativeJdbcExtractor = extractor;
    }

    /**
     * Return the current NativeJdbcExtractor implementation.
     */
    public NativeJdbcExtractor getNativeJdbcExtractor() {
        return this.nativeJdbcExtractor;
    }

    public void setIgnoreWarnings(boolean ignoreWarnings) {
        this.ignoreWarnings = ignoreWarnings;
    }

    public boolean isIgnoreWarnings() {
        return this.ignoreWarnings;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getFetchSize() {
        return this.fetchSize;
    }


    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getMaxRows() {
        return this.maxRows;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }
    //-------------------------------------------------------------------------
    // Methods dealing with static SQL (java.sql.Statement)
    //-------------------------------------------------------------------------

    @Override
    public <T> T execute(StatementCallback<T> action) throws DataAccessException {
        Assert.notNull(action, "Callback object must not be null");

        // 获取数据库连接
        Connection con = DataSourceUtils.getConnection(getDataSource());
        Statement stmt = null;
        try {
            Connection conToUse = con;
            if (this.nativeJdbcExtractor != null &&
                    this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativeStatements()) {
                conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
            }
            stmt = conToUse.createStatement();
            applyStatementSettings(stmt);
            Statement stmtToUse = stmt;
            if (this.nativeJdbcExtractor != null) {
                stmtToUse = this.nativeJdbcExtractor.getNativeStatement(stmt);
            }
            // 调用回调函数(此处使用模板方法模式)
            T result = action.doInStatement(stmtToUse);
            handleWarnings(stmt);
            return result;
        }
        catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            JdbcUtils.closeStatement(stmt);
            stmt = null;
            DataSourceUtils.releaseConnection(con, getDataSource());
            con = null;
            throw getExceptionTranslator().translate("StatementCallback", getSql(action), ex);
        }
        finally {
            JdbcUtils.closeStatement(stmt);
            DataSourceUtils.releaseConnection(con, getDataSource());
        }
    }

    @Override
    public <T> T query(final String sql, final ResultSetExtractor<T> rse) throws DataAccessException {
        Assert.notNull(sql, "SQL must not be null");
        Assert.notNull(rse, "ResultSetExtractor must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL query [" + sql + "]");
        }
        class QueryStatementCallback implements StatementCallback<T>, SqlProvider {
            @Override
            public T doInStatement(Statement stmt) throws SQLException {
                ResultSet rs = null;
                try {
                    rs = stmt.executeQuery(sql);
                    ResultSet rsToUse = rs;
                    if (nativeJdbcExtractor != null) {
                        rsToUse = nativeJdbcExtractor.getNativeResultSet(rs);
                    }
                    return rse.extractData(rsToUse);
                }
                finally {
                    JdbcUtils.closeResultSet(rs);
                }
            }
            @Override
            public String getSql() {
                return sql;
            }
        }
        return execute(new QueryStatementCallback());
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, new RowMapperResultSetExtractor<T>(rowMapper));
    }

    //-------------------------------------------------------------------------
    // Methods dealing with prepared statements
    //-------------------------------------------------------------------------

    @Override
    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
            throws DataAccessException {

        Assert.notNull(psc, "PreparedStatementCreator must not be null");
        Assert.notNull(action, "Callback object must not be null");
        if (logger.isDebugEnabled()) {
            String sql = getSql(psc);
            logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
        }

        // 获取数据库的连接
        Connection con = DataSourceUtils.getConnection(getDataSource());
        PreparedStatement ps = null;
        try {
            Connection conToUse = con;
            if (this.nativeJdbcExtractor != null &&
                    this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativePreparedStatements()) {
                conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
            }
            ps = psc.createPreparedStatement(conToUse);
            // 应用用户输入的参数
            applyStatementSettings(ps);
            PreparedStatement psToUse = ps;
            if (this.nativeJdbcExtractor != null) {
                psToUse = this.nativeJdbcExtractor.getNativePreparedStatement(ps);
            }
            // 调用回调函数(此处使用模板方法模式)
            T result = action.doInPreparedStatement(psToUse);
            handleWarnings(ps);
            return result;
        }
        catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            if (psc instanceof ParameterDisposer) {
                ((ParameterDisposer) psc).cleanupParameters();
            }
            String sql = getSql(psc);
            psc = null;
            JdbcUtils.closeStatement(ps);
            ps = null;
            DataSourceUtils.releaseConnection(con, getDataSource());
            con = null;
            throw getExceptionTranslator().translate("PreparedStatementCallback", sql, ex);
        }
        finally {
            if (psc instanceof ParameterDisposer) {
                ((ParameterDisposer) psc).cleanupParameters();
            }
            JdbcUtils.closeStatement(ps);
            DataSourceUtils.releaseConnection(con, getDataSource());
        }
    }

    protected int update(final PreparedStatementCreator psc, final PreparedStatementSetter pss)
            throws DataAccessException {

        logger.debug("Executing prepared SQL update");
        return execute(psc, new PreparedStatementCallback<Integer>() {
            @Override
            public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException {
                try {
                    if (pss != null) {
                        pss.setValues(ps);
                    }
                    int rows = ps.executeUpdate();
                    if (logger.isDebugEnabled()) {
                        logger.debug("SQL update affected " + rows + " rows");
                    }
                    return rows;
                }
                finally {
                    if (pss instanceof ParameterDisposer) {
                        ((ParameterDisposer) pss).cleanupParameters();
                    }
                }
            }
        });
    }

    @Override
    public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
        return update(new SimplePreparedStatementCreator(sql), pss);
    }
    @Override
    public int update(String sql, Object... args) throws DataAccessException {
        return update(sql, newArgPreparedStatementSetter(args));
    }

    /**
     * Prepare the given JDBC Statement (or PreparedStatement or CallableStatement),
     * applying statement settings such as fetch size, max rows, and query timeout.
     * @param stmt the JDBC Statement to prepare
     * @throws SQLException if thrown by JDBC API
     * @see #setFetchSize
     * @see #setMaxRows
     * @see #setQueryTimeout
     * @see org.springframework.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
     */
    protected void applyStatementSettings(Statement stmt) throws SQLException {
        int fetchSize = getFetchSize();
        if (fetchSize >= 0) {
            stmt.setFetchSize(fetchSize);
        }
        int maxRows = getMaxRows();
        if (maxRows >= 0) {
            stmt.setMaxRows(maxRows);
        }
        DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
    }
    protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
        return new ArgumentPreparedStatementSetter(args);
    }
    /**
     * Throw an SQLWarningException if we're not ignoring warnings,
     * else log the warnings (at debug level).
     * @param stmt the current JDBC statement
     * @throws SQLWarningException if not ignoring warnings
     * @see org.springframework.jdbc.SQLWarningException
     */
    protected void handleWarnings(Statement stmt) throws SQLException {
        if (isIgnoreWarnings()) {
            if (logger.isDebugEnabled()) {
                SQLWarning warningToLog = stmt.getWarnings();
                while (warningToLog != null) {
                    logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" +
                            warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
                    warningToLog = warningToLog.getNextWarning();
                }
            }
        }
        else {
            handleWarnings(stmt.getWarnings());
        }
    }

    protected void handleWarnings(SQLWarning warning) throws SQLWarningException {
        if (warning != null) {
            throw new SQLWarningException("Warning not ignored", warning);
        }
    }

    private static String getSql(Object sqlProvider) {
        if (sqlProvider instanceof SqlProvider) {
            return ((SqlProvider) sqlProvider).getSql();
        }
        else {
            return null;
        }
    }

    /**
     * Simple adapter for PreparedStatementCreator, allowing to use a plain SQL statement.
     */
    private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

        private final String sql;

        public SimplePreparedStatementCreator(String sql) {
            Assert.notNull(sql, "SQL must not be null");
            this.sql = sql;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement(this.sql);
        }

        @Override
        public String getSql() {
            return this.sql;
        }
    }

}
