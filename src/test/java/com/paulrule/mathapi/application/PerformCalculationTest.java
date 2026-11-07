package com.paulrule.mathapi.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.paulrule.mathapi.domain.Calculation;
import com.paulrule.mathapi.domain.Operation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PerformCalculationTest {

    @Test
    void returnsTheNormalisedResult() {
        var calculation = new Calculation(Operation.MULTIPLY, new BigDecimal("0.10"), new BigDecimal("10"));

        assertThat(new PerformCalculation().perform(calculation)).isEqualTo(new BigDecimal("1"));
    }
}
