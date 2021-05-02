package io.github.ulisse1996.entity.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SqlGetter<R> {

    R get(ResultSet rs, String colName) throws SQLException;
}
