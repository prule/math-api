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
    },
    /**
     * {@code a ^ b} for an integer b with |b| ≤ 999 (BR-5): the exact power rounded once to 34 significant digits,
     * HALF_EVEN (BR-3); zero to a negative power is rejected (BR-2); a Result outside ±1000 scale is rejected (BR-6).
     */
    POWER {
        @Override
        public BigDecimal apply(BigDecimal a, BigDecimal b) {
            int n = exponent(b);
            if (n < 0 && a.signum() == 0) {
                throw new DivisionByZeroException();
            }
            // Exact first, rounded once: pow(n, MathContext) only promises two ulps, BR-3 promises HALF_EVEN.
            var result = n >= 0
                    ? a.pow(n).round(MathContext.DECIMAL128)
                    : BigDecimal.ONE.divide(a.pow(-n), MathContext.DECIMAL128);
            if (Math.abs((long) result.stripTrailingZeros().scale())
                    > Calculation.MAX_SCALE) { // BR-6 reuses the BR-1 bound
                throw new ResultOutOfRangeException();
            }
            return result;
        }
    },
    /** {@code a mod b}: exact remainder truncated toward zero, so it takes the sign of a (BR-7); zero b rejected (BR-2). */
    MODULO {
        @Override
        public BigDecimal apply(BigDecimal a, BigDecimal b) {
            if (b.signum() == 0) {
                throw new DivisionByZeroException();
            }
            return a.remainder(b);
        }
    };

    public abstract BigDecimal apply(BigDecimal a, BigDecimal b);

    private static final BigDecimal MAX_EXPONENT = BigDecimal.valueOf(999);

    /** BR-5: b must be an integer (3.0 counts) with |b| ≤ 999. */
    private static int exponent(BigDecimal b) {
        if (b.stripTrailingZeros().scale() > 0 || b.abs().compareTo(MAX_EXPONENT) > 0) {
            throw new InvalidExponentException();
        }
        return b.intValueExact();
    }

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
