package com.paulrule.mathapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.paulrule.mathapi.domain.OperandOutOfRangeException.Limit;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    }

    // --- BR-9: Operand count matches the Operation's arity ---

    @Test
    void binaryOperationWithoutBIsRejected_BR9() {
        var ex = catchThrowableOfType(
                ArityMismatchException.class, () -> new Calculation(Operation.ADD, BigDecimal.ONE, null));

        assertThat(ex.operation()).isEqualTo(Operation.ADD);
    }

    @ParameterizedTest
    @EnumSource(
            value = Operation.class,
            names = {"SQRT", "NEGATE"})
    void unaryOperationWithBIsRejected_BR9(Operation unary) {
        var ex = catchThrowableOfType(
                ArityMismatchException.class, () -> new Calculation(unary, BigDecimal.ONE, BigDecimal.ONE));

        assertThat(ex.operation()).isEqualTo(unary);
    }

    @Test
    void unaryOperationWithoutBIsAccepted_BR9() {
        assertThat(new Calculation(Operation.NEGATE, BigDecimal.ONE, null).result())
                .isEqualByComparingTo("-1");
    }

    @Test
    void arityIsCheckedBeforeOperandLimits_BR9() {
        assertThatThrownBy(() -> new Calculation(Operation.SQRT, new BigDecimal("4"), new BigDecimal("1E+1001")))
                .isInstanceOf(ArityMismatchException.class);
    }

    // --- BR-1: operand limits, checked on the Operand as written ---

    private static final String DIGITS_100 = "1234567890".repeat(10);
    private static final String DIGITS_101 = DIGITS_100 + "1";
    private static final String ONE_AND_100_ZEROS = "1" + "0".repeat(100);

    @ParameterizedTest
    @MethodSource("withinLimits")
    void operandsAtTheLimitsAreAccepted_BR1(String operand) {
        assertThatCode(() -> calc(Operation.ADD, operand, operand)).doesNotThrowAnyException();
    }

    static String[] withinLimits() {
        return new String[] {DIGITS_100, "1E+1000", "1E-1000", "1E+100"};
    }

    @ParameterizedTest
    @MethodSource("pastLimits")
    void operandsPastALimitAreRejected_BR1(String operand, Limit limit) {
        var onA = catchThrowableOfType(OperandOutOfRangeException.class, () -> calc(Operation.ADD, operand, "1"));
        var onB = catchThrowableOfType(OperandOutOfRangeException.class, () -> calc(Operation.ADD, "1", operand));

        assertThat(onA.operand()).isEqualTo("a");
        assertThat(onA.limit()).isEqualTo(limit);
        assertThat(onB.operand()).isEqualTo("b");
        assertThat(onB.limit()).isEqualTo(limit);
        assertThat(onA.getMessage()).doesNotContain(operand);
    }

    static Object[][] pastLimits() {
        return new Object[][] {
            {DIGITS_101, Limit.DIGITS},
            {ONE_AND_100_ZEROS, Limit.DIGITS},
            {"1E+1001", Limit.SCALE},
            {"1E-1001", Limit.SCALE},
            {"1E+2147483648", Limit.SCALE}, // scale == Integer.MIN_VALUE
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e999999999", "1e-999999999"})
    void extremeExponentsAreRejectedBeforeAnyArithmetic_BR1(String operand) {
        var ex = assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> catchThrowableOfType(
                        OperandOutOfRangeException.class, () -> calc(Operation.MULTIPLY, operand, operand)));

        assertThat(ex).isNotNull();
        assertThat(ex.limit()).isEqualTo(Limit.SCALE);
    }

    @Test
    void firstOperandIsReportedWhenBothAreOutOfRange() {
        var ex = catchThrowableOfType(
                OperandOutOfRangeException.class, () -> calc(Operation.ADD, "1E+1001", DIGITS_101));

        assertThat(ex.operand()).isEqualTo("a");
    }
}
