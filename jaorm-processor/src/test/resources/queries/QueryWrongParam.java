package io.test;

import io.github.ulisse1996.annotation.Query;

public interface QueryWrongParam {

    @Query(sql = "SELECT I FROM O WHERE E = ? AND ER = ?")
    String getFirst(String e);
}
