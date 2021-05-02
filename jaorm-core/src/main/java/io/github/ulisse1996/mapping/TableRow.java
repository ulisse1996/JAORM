package io.github.ulisse1996.mapping;

import io.github.ulisse1996.ResultSetExecutor;
import io.github.ulisse1996.SqlUtil;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableRow implements Closeable {

    private final Connection connection;
    private final PreparedStatement preparedStatement;
    private final ResultSetExecutor resultSetExecutor;
    private final boolean delegated;

    public TableRow(Connection connection, PreparedStatement preparedStatement, ResultSetExecutor resultSet) {
        this(connection, preparedStatement, resultSet, false);
    }

    public TableRow(Connection connection, PreparedStatement preparedStatement, ResultSetExecutor resultSet, boolean delegated) {
        this.connection = connection;
        this.preparedStatement = preparedStatement;
        this.resultSetExecutor = resultSet;
        this.delegated = delegated;
    }

    public <R> R mapRow(RowMapper<R> mapper) throws SQLException {
        resultSetExecutor.getResultSet().next();
        R val = mapper.map(resultSetExecutor.getResultSet());
        close();
        return val;
    }

    @Override
    public void close() {
        if (!delegated) {
            SqlUtil.silentClose(resultSetExecutor, preparedStatement, connection);
        }
    }
}
