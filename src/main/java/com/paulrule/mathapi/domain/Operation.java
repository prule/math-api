package com.paulrule.mathapi.domain;

import java.math.BigDecimal;

/** A named arithmetic function of two Operands. Closed set; see docs/DOMAIN-MODEL.md §3. */
public enum Operation {
    /** {@code a + b}, exact (BR-3). */
    ADD {
        @Override
        public BigDecimal apply(BigDecimal a, BigDecimal b) {
            return a.add(b);
        }
    },
    /** {@code a − b}, exact (BR-3). */
    SUBTRACT {
        @Override
        public BigDecimal apply(BigDecimal a, BigDecimal b) {
            return a.subtract(b);
        }
    },
    /** {@code a × b}, exact (BR-3). */
    MULTIPLY {
        @Override
        public BigDecimal apply(BigDecimal a, BigDecimal b) {
            return a.multiply(b);
        }
    };

    public abstract BigDecimal apply(BigDecimal a, BigDecimal b);

    /** Exact, case-sensitive lookup by name. */
    public static Operation named(String name) {
        if (name != null) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                // fall through
            }
        }
        throw new UnknownOperationException(name);
    }
}
