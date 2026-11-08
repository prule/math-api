package com.paulrule.mathapi.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** One Operation applied to its Operands; b is null for a unary Operation (BR-9). */
public record Calculation(Operation operation, BigDecimal a, BigDecimal b) {

    /** BR-1, applied to the Operand as written so both the arithmetic and the echoed operands stay bounded. */
    public static final int MAX_DIGITS = 100;

    public static final int MAX_SCALE = 1000;

    public Calculation {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(a, "a");
        if ((b == null) != (operation.arity() == 1)) {
            throw new ArityMismatchException(operation);
        }
        requireWithinLimits("a", a);
        if (b != null) {
            requireWithinLimits("b", b);
        }
    }

    private static void requireWithinLimits(String name, BigDecimal operand) {
        if (operand.precision() > MAX_DIGITS) {
            throw new OperandOutOfRangeException(name, OperandOutOfRangeException.Limit.DIGITS);
        }
        // long, not int: "1E+2147483648" has scale Integer.MIN_VALUE, and Math.abs(int) of that stays negative.
        if (Math.abs((long) operand.scale()) > MAX_SCALE) {
            throw new OperandOutOfRangeException(name, OperandOutOfRangeException.Limit.SCALE);
        }
    }

    /** The Result, normalised per BR-4 (trailing zeros stripped; zero has scale 0). */
    public BigDecimal result() {
        return operation.apply(a, b).stripTrailingZeros();
    }
}
