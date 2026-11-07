package com.paulrule.mathapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OperationTest {

    @ParameterizedTest(name = "{0} {1} {2} = {3}")
    @CsvSource({
        // ADD (BR-3: exact)
        "ADD, 0.1, 0.2, 0.3",
        "ADD, -5, 3, -2",
        "ADD, -1.25, -0.75, -2",
        "ADD, 12345678901234567890.123456789, 0.000000001, 12345678901234567890.12345679",
        // SUBTRACT
        "SUBTRACT, 1.10, 1.1, 0",
        "SUBTRACT, 2, 5.5, -3.5",
        // MULTIPLY
        "MULTIPLY, 123.45, 0, 0",
        "MULTIPLY, -2, -3, 6",
        "MULTIPLY, -2, 3, -6",
        "MULTIPLY, 1.5, 1.5, 2.25",
        "MULTIPLY, 0.001, 0.001, 0.000001",
    })
    void appliesExactly(Operation operation, String a, String b, String expected) {
        assertThat(operation.apply(new BigDecimal(a), new BigDecimal(b))).isEqualByComparingTo(expected);
    }

    @Test
    void addKeepsEveryDigitOfLargePrecisionOperands() {
        var result =
                Operation.ADD.apply(new BigDecimal("12345678901234567890.123456789"), new BigDecimal("0.000000001"));

        assertThat(result.toPlainString()).isEqualTo("12345678901234567890.123456790");
    }

    @Test
    void namedFindsExactName() {
        assertThat(Operation.named("ADD")).isEqualTo(Operation.ADD);
    }

    @Test
    void namedIsCaseSensitive() {
        assertThatThrownBy(() -> Operation.named("add"))
                .isInstanceOf(UnknownOperationException.class)
                .extracting("name")
                .isEqualTo("add");
    }

    @Test
    void namedRejectsUnsupportedOperation() {
        assertThatThrownBy(() -> Operation.named("POWER")).isInstanceOf(UnknownOperationException.class);
    }

    @Test
    void namedRejectsNull() {
        assertThatThrownBy(() -> Operation.named(null)).isInstanceOf(UnknownOperationException.class);
    }
}
