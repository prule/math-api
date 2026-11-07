package com.paulrule.mathapi.domain;

/** A POWER Result whose normalised scale is outside -1000 to 1000 (BR-6). */
public class ResultOutOfRangeException extends RuntimeException {

    public ResultOutOfRangeException() {
        super("Result scale outside -1000 to 1000 (BR-6)");
    }
}
