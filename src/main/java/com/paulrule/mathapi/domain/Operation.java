package com.paulrule.mathapi.domain;

import java.math.BigDecimal;
import java.math.MathContext;

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
    },
    /** {@code a ÷ b} to 34 significant digits, HALF_EVEN (BR-3); a zero divisor is rejected (BR-2). */
    DIVIDE {
        @Override
        public BigDecimal apply(BigDecimal a, BigDecimal b) {
            // signum() catches every zero form (0, 0.00, 0E+5); checked first so BigDecimal never throws.
            if (b.signum() == 0) {
                throw new DivisionByZeroException();
            }
            return a.divide(b, MathContext.DECIMAL128);
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
