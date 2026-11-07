package com.paulrule.mathapi.domain;

/** A POWER exponent that is not an integer or whose absolute value exceeds 999 (BR-5). */
public class InvalidExponentException extends RuntimeException {

    public InvalidExponentException() {
        super("Exponent must be an integer between -999 and 999 (BR-5)");
    }
}
