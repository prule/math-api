package com.paulrule.mathapi.domain;

/** Operand b given to a unary Operation, or missing for a binary one (BR-9). */
public class ArityMismatchException extends RuntimeException {

    private final Operation operation;

    public ArityMismatchException(Operation operation) {
        super(operation + " takes " + operation.arity() + " operand(s) (BR-9)");
        this.operation = operation;
    }

    public Operation operation() {
        return operation;
    }
}
