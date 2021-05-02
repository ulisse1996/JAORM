package io.github.ulisse1996.integration.test;

import io.github.ulisse1996.entity.sql.DataSourceProvider;
import org.hsqldb.jdbc.JDBCDataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

public class HSQLDBProvider extends DataSourceProvider {

    private DataSource dataSource;

    public void set(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource() {
        if (this.dataSource == null) {
            createFor(DatabaseType.ORACLE);
        }

        return this.dataSource;
    }

    public void createFor(DatabaseType type) {
        this.dataSource = createDatasource(type);
    }

    private static DataSource createDatasource(DatabaseType type) {
        try {
            Properties prop = new Properties();
            prop.put("url", "jdbc:hsqldb:mem:jaorm;" + type.getSyntax());
            prop.put("user", "jaorm");
            prop.put("password", "");
            return JDBCDataSourceFactory.createDataSource(prop);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public enum DatabaseType {
        ORACLE("sql.syntax_ora=true"),
        POSTGRE("sql.syntax_pgs=true"),
        MYSQL("sql.syntax_mys=true"),
        DB2("sql.syntax_db2=true"),
        MS_SQLSERVER("sql.syntax_mss=true");

        private final String syntax;

        DatabaseType(String syntax) {
            this.syntax = syntax;
        }

        public String getSyntax() {
            return syntax;
        }
    }
}
