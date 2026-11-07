package com.paulrule.mathapi.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** One Operation applied to two Operands. */
public record Calculation(Operation operation, BigDecimal a, BigDecimal b) {

    public Calculation {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
    }

    /** The Result, normalised per BR-4 (trailing zeros stripped; zero has scale 0). */
    public BigDecimal result() {
        return operation.apply(a, b).stripTrailingZeros();
    }
}
