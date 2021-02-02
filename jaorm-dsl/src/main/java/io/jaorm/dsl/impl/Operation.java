package io.jaorm.dsl.impl;

public enum Operation {
    EQUALS(" = "),
    NOT_EQUALS(" <> "),
    LESS_THAN(" < "),
    GREATER_THAN(" > "),
    LESS_EQUALS(" <= "),
    GREATER_EQUALS(" >= "),
    IS_NULL(""),
    IS_NOT_NULL(""),
    IN(""),
    NOT_IN(""),
    LIKE("");


    private final String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}