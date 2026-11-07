package com.paulrule.mathapi.domain;

/** An Operand outside BR-1. Carries which operand and which limit, never the value (it may be huge). */
public class OperandOutOfRangeException extends RuntimeException {

    public enum Limit {
        DIGITS,
        SCALE
    }

    private final String operand;
    private final Limit limit;

    public OperandOutOfRangeException(String operand, Limit limit) {
        super("Operand '" + operand + "' is outside the " + limit + " limit (BR-1)");
        this.operand = operand;
        this.limit = limit;
    }

    public String operand() {
        return operand;
    }

    public Limit limit() {
        return limit;
    }
}
