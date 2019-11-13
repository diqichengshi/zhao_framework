package org.springframework.jdbc.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * Base class for {@link org.springframework.jdbc.core.JdbcTemplate} and
 * other JDBC-accessing DAO helpers, defining common properties such as
 * DataSource and exception translator.
 *
 * <p>Not intended to be used directly.
 * See {@link org.springframework.jdbc.core.JdbcTemplate}.
 *
 * @author Juergen Hoeller
 * @since 28.11.2003
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public abstract class JdbcAccessor implements InitializingBean {
    protected final Log logger = LogFactory.getLog(getClass());

    private DataSource dataSource;

    private SQLExceptionTranslator exceptionTranslator;

    private boolean lazyInit = true;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public void setDatabaseProductName(String dbName) {
        this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
    }

    public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator;
    }

    public synchronized SQLExceptionTranslator getExceptionTranslator() {
        if (this.exceptionTranslator == null) {
            DataSource dataSource = getDataSource();
            if (dataSource != null) {
                this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
            }
            else {
                this.exceptionTranslator = new SQLStateSQLExceptionTranslator();
            }
        }
        return this.exceptionTranslator;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public boolean isLazyInit() {
        return this.lazyInit;
    }

    @Override
    public void afterPropertiesSet() {
        if (getDataSource() == null) {
            throw new IllegalArgumentException("Property 'dataSource' is required");
        }
        if (!isLazyInit()) {
            getExceptionTranslator();
        }
    }

}
