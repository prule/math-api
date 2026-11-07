package com.paulrule.mathapi.domain;

/** A DIVIDE whose divisor is zero in any representation (BR-2). */
public class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException() {
        super("Division by zero");
    }
}
