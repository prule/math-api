package com.paulrule.mathapi.domain;

/** SQRT of an Operand below zero (BR-8). */
public class NegativeSquareRootException extends RuntimeException {

    public NegativeSquareRootException() {
        super("SQRT needs a non-negative operand (BR-8)");
    }
}
