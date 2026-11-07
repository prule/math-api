package com.paulrule.mathapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculationTest {

    private static Calculation calc(Operation op, String a, String b) {
        return new Calculation(op, new BigDecimal(a), new BigDecimal(b));
    }

    @Test
    void resultHasTrailingZerosStripped_BR4() {
        var result = calc(Operation.MULTIPLY, "0.10", "10").result();

        assertThat(result).isEqualTo(new BigDecimal("1"));
        assertThat(result.scale()).isZero();
    }

    @Test
    void zeroResultIsPlainZero_BR4() {
        var result = calc(Operation.SUBTRACT, "1.10", "1.1").result();

        assertThat(result).isEqualTo(BigDecimal.ZERO);
        assertThat(result.toPlainString()).isEqualTo("0");
    }

    @Test
    void resultRendersInPlainNotation_BR4() {
        var result = calc(Operation.MULTIPLY, "1000", "1000").result();

        assertThat(result.toPlainString()).isEqualTo("1000000");
    }

    @Test
    void rejectsNullComponents() {
        assertThatNullPointerException().isThrownBy(() -> new Calculation(null, BigDecimal.ONE, BigDecimal.ONE));
        assertThatNullPointerException().isThrownBy(() -> new Calculation(Operation.ADD, null, BigDecimal.ONE));
        assertThatNullPointerException().isThrownBy(() -> new Calculation(Operation.ADD, BigDecimal.ONE, null));
    }
}
