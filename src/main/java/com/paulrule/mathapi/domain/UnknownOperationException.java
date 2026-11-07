package com.paulrule.mathapi.domain;

/** The requested name is not exactly the name of a supported Operation. */
public class UnknownOperationException extends RuntimeException {

    private final String name;

    public UnknownOperationException(String name) {
        super("Unknown operation: " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
