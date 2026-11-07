package com.paulrule.mathapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
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
        // DIVIDE (BR-3: 34 significant digits, HALF_EVEN)
        "DIVIDE, 1, 4, 0.25",
        "DIVIDE, 1, 3, 0.3333333333333333333333333333333333",
        "DIVIDE, 2, 3, 0.6666666666666666666666666666666667",
        "DIVIDE, 1, -4, -0.25",
        "DIVIDE, 0, 5, 0",
        // MODULO (BR-7: truncated remainder, sign of a, exact)
        "MODULO, -7, 3, -1",
        "MODULO, 7, -3, 1",
        "MODULO, 5.5, 2, 1.5",
        "MODULO, -7.5, 2, -1.5",
        "MODULO, 6, 3, 0",
        "MODULO, 1E+1000, 7, 4",
    })
    void applies(Operation operation, String a, String b, String expected) {
        assertThat(operation.apply(new BigDecimal(a), new BigDecimal(b))).isEqualByComparingTo(expected);
    }

    @Test
    void addKeepsEveryDigitOfLargePrecisionOperands() {
        var result =
                Operation.ADD.apply(new BigDecimal("12345678901234567890.123456789"), new BigDecimal("0.000000001"));

        assertThat(result.toPlainString()).isEqualTo("12345678901234567890.123456790");
    }

    @ParameterizedTest(name = "{0} / 1 = {1}")
    @CsvSource({
        // 35 significant digits, tie on the 35th: HALF_EVEN rounds to the even neighbour (HALF_UP would not)
        "1.0000000000000000000000000000000005, 1",
        "1.0000000000000000000000000000000015, 1.000000000000000000000000000000002",
    })
    void divideRoundsTiesToEven(String a, String expected) {
        var result = Operation.DIVIDE.apply(new BigDecimal(a), BigDecimal.ONE);

        assertThat(result.stripTrailingZeros().toPlainString()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({"1, 0", "1, 0.00", "1, -0", "1, 0E+5", "0, 0"})
    void divideByAnyZeroIsRejected_BR2(String a, String b) {
        assertThatThrownBy(() -> Operation.DIVIDE.apply(new BigDecimal(a), new BigDecimal(b)))
                .isInstanceOf(DivisionByZeroException.class);
    }

    @ParameterizedTest(name = "7 mod {0}")
    @CsvSource({"0", "0.00"})
    void moduloByZeroIsRejected_BR2(String b) {
        assertThatThrownBy(() -> Operation.MODULO.apply(new BigDecimal("7"), new BigDecimal(b)))
                .isInstanceOf(DivisionByZeroException.class);
    }

    // --- POWER (BR-3 rounding, BR-5 exponent, BR-6 result bound) ---

    private static String power(String a, String b) {
        return Operation.POWER
                .apply(new BigDecimal(a), new BigDecimal(b))
                .stripTrailingZeros()
                .toPlainString();
    }

    @ParameterizedTest(name = "{0} ^ {1} = {2}")
    @CsvSource({
        "2, 10, 1024",
        "2, -2, 0.25",
        "3, -1, 0.3333333333333333333333333333333333",
        "-2, 3, -8",
        "1.1, 2, 1.21",
        "2, 200, 1606938044258990275541962092341163000000000000000000000000000",
        "0, 0, 1",
        "5, 0, 1",
        "0, 5, 0",
        "2, 3.0, 8",
    })
    void powerIsExactThenRoundedTo34Digits(String a, String b, String expected) {
        assertThat(power(a, b)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "2 ^ {0}")
    @CsvSource({"0.5", "1000", "-1000", "1E+3"})
    void powerRejectsNonIntegerOrUnboundedExponent_BR5(String b) {
        assertThatThrownBy(() -> power("2", b)).isInstanceOf(InvalidExponentException.class);
    }

    @Test
    void zeroToANegativePowerIsDivisionByZero_BR2() {
        assertThatThrownBy(() -> power("0", "-1")).isInstanceOf(DivisionByZeroException.class);
    }

    @Test
    void exponentRuleIsCheckedBeforeZeroBase() {
        assertThatThrownBy(() -> power("0", "-0.5")).isInstanceOf(InvalidExponentException.class);
    }

    @ParameterizedTest(name = "{0} ^ 999")
    @CsvSource({"100", "0.01"})
    void powerResultOutsideScaleBoundIsRejected_BR6(String a) {
        assertThatThrownBy(() -> power(a, "999")).isInstanceOf(ResultOutOfRangeException.class);
    }

    @ParameterizedTest(name = "{0} ^ 999")
    @CsvSource({"10", "0.1"})
    void powerResultAtScaleBoundIsAccepted_BR6(String a) {
        assertThat(new BigDecimal(power(a, "999")).stripTrailingZeros().scale()).isBetween(-1000, 1000);
    }

    private static final String WORST_OPERAND = "9".repeat(100) + "E-1000"; // 100 digits, scale 1000

    @ParameterizedTest(name = "worst ^ {0}")
    @CsvSource({"999", "-999"})
    void worstCasePowerIsRejectedQuickly(String b) {
        assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> assertThatThrownBy(() -> power(WORST_OPERAND, b)).isInstanceOf(ResultOutOfRangeException.class));
    }

    @Test
    void heaviestInRangePowerIsFast() {
        var nearOne = "1." + "0".repeat(98) + "1"; // 100 significant digits
        assertThat(assertTimeoutPreemptively(Duration.ofSeconds(1), () -> power(nearOne, "999")))
                .isEqualTo("1");
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
        assertThatThrownBy(() -> Operation.named("NOT_AN_OPERATION")).isInstanceOf(UnknownOperationException.class);
    }

    @Test
    void namedRejectsNull() {
        assertThatThrownBy(() -> Operation.named(null)).isInstanceOf(UnknownOperationException.class);
    }
}
