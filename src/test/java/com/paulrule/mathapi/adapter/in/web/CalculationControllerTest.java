package com.paulrule.mathapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paulrule.mathapi.config.UseCaseConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CalculationController.class)
@Import(UseCaseConfig.class)
class CalculationControllerTest {

    @Autowired
    MockMvc mvc;

    private ResultActions postJson(String body) throws Exception {
        return mvc.perform(post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // --- happy path (spec: Perform a Calculation, Operands accepted as numbers or strings, Operands echoed) ---

    @Test
    void performsCalculationAndEchoesRequest() throws Exception {
        postJson("""
                        {"operation":"ADD","a":"2","b":"3"}""").andExpect(status().isOk()).andExpect(content().json("""
                        {"operation":"ADD","a":"2","b":"3","result":"5"}""", JsonCompareMode.STRICT));
    }

    @Test
    void acceptsNumberAndStringOperands() throws Exception {
        postJson("""
                        {"operation":"ADD","a":1.5,"b":"2.5"}""").andExpect(status().isOk()).andExpect(jsonPath("$.result").value("4"));
    }

    @Test
    void readsJsonNumbersExactly() throws Exception {
        postJson("""
                        {"operation":"ADD","a":0.1,"b":0.2}""").andExpect(jsonPath("$.result").value("0.3"));
    }

    @Test
    void echoesOperandsInPlainNotationKeepingScale() throws Exception {
        postJson("""
                        {"operation":"ADD","a":1e2,"b":"1.50"}""")
                .andExpect(jsonPath("$.a").value("100"))
                .andExpect(jsonPath("$.b").value("1.50"));
    }

    @ParameterizedTest(name = "{0} {1} {2} = {3}")
    @CsvSource({
        "ADD, 12345678901234567890.123456789, 0.000000001, 12345678901234567890.12345679",
        "SUBTRACT, 2, 5.5, -3.5",
        "MULTIPLY, 0.10, 10, 1",
        "MULTIPLY, 1000, 1000, 1000000",
        "DIVIDE, 1, 3, 0.3333333333333333333333333333333333",
        "DIVIDE, 1, 4, 0.25",
        "POWER, 2, 10, 1024",
        "POWER, 2, -2, 0.25",
        "POWER, 3, -1, 0.3333333333333333333333333333333333",
        "POWER, -2, 3, -8",
        "POWER, 1.1, 2, 1.21",
        "POWER, 2, 200, 1606938044258990275541962092341163000000000000000000000000000",
        "POWER, 0, 0, 1",
        "POWER, 5, 0, 1",
        "POWER, 2, 3.0, 8",
        "MODULO, -7, 3, -1",
        "MODULO, 7, -3, 1",
        "MODULO, 5.5, 2, 1.5",
        "MODULO, -7.5, 2, -1.5",
        "MODULO, 6, 3, 0",
        "MODULO, 1E+1000, 7, 4",
    })
    void eachOperationReturnsNormalisedPlainResult(String op, String a, String b, String expected) throws Exception {
        postJson("{\"operation\":\"%s\",\"a\":\"%s\",\"b\":\"%s\"}".formatted(op, a, b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value(op))
                .andExpect(jsonPath("$.result").value(expected));
    }

    // --- errors (spec: Invalid request rejected, Unknown Operation rejected, Error responses use Problem Details) ---

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "malformed JSON       | {\"operation\":\"ADD\",\"a\":1,",
                "missing operand      | {\"operation\":\"ADD\",\"a\":\"1\"}",
                "null operand         | {\"operation\":\"ADD\",\"a\":null,\"b\":\"2\"}",
                "missing operation    | {\"a\":\"1\",\"b\":\"2\"}",
                "non-numeric operand  | {\"operation\":\"ADD\",\"a\":\"abc\",\"b\":\"2\"}",
                "boolean operand      | {\"operation\":\"ADD\",\"a\":true,\"b\":\"2\"}",
                "unary missing a      | {\"operation\":\"SQRT\"}",
            })
    void rejectsInvalidRequest(String caseName, String body) throws Exception {
        expectProblem(postJson(body), 400, "/problems/invalid-request");
    }

    @ParameterizedTest
    @CsvSource({"NOT_AN_OPERATION", "add"})
    void rejectsUnknownOperation(String operation) throws Exception {
        expectProblem(
                        postJson("{\"operation\":\"%s\",\"a\":\"2\",\"b\":\"3\"}".formatted(operation)),
                        400,
                        "/problems/unknown-operation")
                .andExpect(jsonPath("$.detail").value(containsString(operation)));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "\"1\" | \"0\"",
                "\"1\" | \"0.00\"",
                "\"1\" | \"-0\"",
                "\"1\" | \"0E+5\"",
                "\"1\" | 0.0",
                "\"0\" | \"0\""
            })
    void rejectsDivisionByZero(String dividend, String divisor) throws Exception {
        expectProblem(
                postJson("{\"operation\":\"DIVIDE\",\"a\":%s,\"b\":%s}".formatted(dividend, divisor)),
                422,
                "/problems/division-by-zero");
    }

    // --- BR-1 operand limits (spec: Operand digits/scale limited, detail names operand, validation order) ---

    private static final String DIGITS_100 = "1234567890".repeat(10);

    private ResultActions calculate(String operation, String a, String b) throws Exception {
        return postJson("{\"operation\":\"%s\",\"a\":%s,\"b\":%s}".formatted(operation, a, b));
    }

    private static String quoted(String s) {
        return "\"" + s + "\"";
    }

    @Test
    void acceptsExactly100Digits() throws Exception {
        calculate("ADD", quoted(DIGITS_100), quoted("0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(DIGITS_100));
    }

    @Test
    void rejects101Digits() throws Exception {
        expectProblem(calculate("ADD", quoted(DIGITS_100 + "1"), quoted("0")), 400, "/problems/operand-out-of-range");
    }

    @Test
    void trailingZerosWrittenOutCountAsDigits() throws Exception {
        expectProblem(
                calculate("ADD", quoted("1" + "0".repeat(100)), quoted("0")), 400, "/problems/operand-out-of-range");
        calculate("ADD", quoted("1E+100"), quoted("0")).andExpect(status().isOk());
    }

    @Test
    void acceptsScaleLimits() throws Exception {
        calculate("ADD", quoted("1E+1000"), quoted("1E-1000")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"\"1E+1001\" | \"1\"", "\"1\" | \"1E-1001\""})
    void rejectsOnePastEachScaleLimit(String a, String b) throws Exception {
        expectProblem(calculate("ADD", a, b), 400, "/problems/operand-out-of-range");
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"MULTIPLY | 1e999999999 | 1e999999999", "DIVIDE | \"1e-999999999\" | \"1e999999999\""})
    void rejectsExtremeExponentsQuickly(String operation, String a, String b) {
        assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> expectProblem(calculate(operation, a, b), 400, "/problems/operand-out-of-range"));
    }

    // Exponents at or past the int scale boundary are rejected by Jackson's parser, as number or string, before the
    // domain sees them; the domain's own guard for scale == Integer.MIN_VALUE is pinned in CalculationTest.
    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"1E+2147483648", "\"1E+2147483648\"", "1e9999999999", "\"1e9999999999\""})
    void rejectsHugeExponentsWhileParsing(String operand) throws Exception {
        expectProblem(calculate("ADD", operand, quoted("1")), 400, "/problems/invalid-request");
    }

    @Test
    void outOfRangeDetailNamesOperandAndLimitButNotValue() throws Exception {
        expectProblem(calculate("ADD", quoted("1"), quoted("1E+1001")), 400, "/problems/operand-out-of-range")
                .andExpect(jsonPath("$.detail").value(containsString("'b'")))
                .andExpect(jsonPath("$.detail").value(containsString("scale")))
                .andExpect(jsonPath("$.detail").value(not(containsString("1E+1001"))));
    }

    @Test
    void unknownOperationWinsOverOperandLimits() throws Exception {
        expectProblem(
                calculate("NOT_AN_OPERATION", quoted("1E+1001"), quoted("1")), 400, "/problems/unknown-operation");
    }

    @Test
    void operandLimitsWinOverDivisionByZero() throws Exception {
        expectProblem(calculate("DIVIDE", quoted("1E+1001"), quoted("0")), 400, "/problems/operand-out-of-range");
    }

    // --- bounded request parsing (spec: Request parsing is bounded) ---

    @Test
    void rejectsOversizedBody() throws Exception {
        var padding = "x".repeat(17_000);
        expectProblem(
                postJson("{\"operation\":\"ADD\",\"a\":\"1\",\"b\":\"2\",\"padding\":\"%s\"}".formatted(padding)),
                400,
                "/problems/invalid-request");
    }

    @Test
    void rejectsOversizedStringOperand() throws Exception {
        expectProblem(calculate("ADD", quoted("9".repeat(1001)), quoted("1")), 400, "/problems/invalid-request");
    }

    @Test
    void rejectsOversizedNumberOperand() throws Exception {
        expectProblem(calculate("ADD", "9".repeat(1001), quoted("1")), 400, "/problems/invalid-request");
    }

    @Test
    void rejectsOversizedStringField() throws Exception {
        // Without max-string-length this would be a 400 unknown-operation instead.
        expectProblem(calculate("A".repeat(1001), quoted("1"), quoted("2")), 400, "/problems/invalid-request");
    }

    @Test
    void largeButParseableOperandReachesTheDomainRule() throws Exception {
        expectProblem(calculate("ADD", quoted("9".repeat(150)), quoted("1")), 400, "/problems/operand-out-of-range");
    }

    // --- POWER and MODULO rules (spec: POWER exponent, POWER result bound, Division by zero, Validation order) ---

    @ParameterizedTest(name = "2 ^ {0}")
    @CsvSource({"0.5", "1000", "-1000"})
    void rejectsInvalidExponent(String exponent) throws Exception {
        expectProblem(calculate("POWER", quoted("2"), quoted(exponent)), 422, "/problems/invalid-exponent");
    }

    @ParameterizedTest(name = "{0} ^ 999")
    @CsvSource({"10", "0.1"})
    void acceptsExponentAtTheBound(String base) throws Exception {
        calculate("POWER", quoted(base), quoted("999")).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} ^ 999")
    @CsvSource({"100", "0.01"})
    void rejectsResultOutOfRange(String base) throws Exception {
        expectProblem(calculate("POWER", quoted(base), quoted("999")), 422, "/problems/result-out-of-range");
    }

    @ParameterizedTest(name = "MODULO 7 by {0}")
    @CsvSource({"0", "0.00"})
    void rejectsModuloByZero(String divisor) throws Exception {
        expectProblem(calculate("MODULO", quoted("7"), quoted(divisor)), 422, "/problems/division-by-zero");
    }

    @Test
    void rejectsZeroToANegativePower() throws Exception {
        expectProblem(calculate("POWER", quoted("0"), quoted("-1")), 422, "/problems/division-by-zero");
    }

    @Test
    void exponentRuleWinsOverZeroToANegativePower() throws Exception {
        expectProblem(calculate("POWER", quoted("0"), quoted("-0.5")), 422, "/problems/invalid-exponent");
    }

    // E notation on purpose: written plainly this operand is 1,002 characters and the string limit would reject it.
    private static final String WORST_OPERAND = "9." + "9".repeat(99) + "E-901"; // 100 digits, scale 1000

    @ParameterizedTest(name = "worst ^ {0}")
    @CsvSource({"999", "-999"})
    void worstCasePowerIsRejectedQuickly(String exponent) {
        assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> expectProblem(
                        calculate("POWER", quoted(WORST_OPERAND), quoted(exponent)),
                        422,
                        "/problems/result-out-of-range"));
    }

    @Test
    void heaviestInRangePowerIsFast() {
        var nearOne = "1." + "0".repeat(98) + "1";
        assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> calculate("POWER", quoted(nearOne), quoted("999"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result").value("1")));
    }

    // --- unary Operations (spec: Perform a Calculation, Operand count, NEGATE, SQRT, square root of a negative) ---

    private ResultActions calculateUnary(String operation, String a) throws Exception {
        return postJson("{\"operation\":\"%s\",\"a\":%s}".formatted(operation, a));
    }

    @Test
    void unaryResponseHasNoBKey() throws Exception {
        calculateUnary("NEGATE", quoted("2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"operation":"NEGATE","a":"2","result":"-2"}""", JsonCompareMode.STRICT));
    }

    @Test
    void nullBCountsAsOmittedForUnary() throws Exception {
        calculate("SQRT", quoted("4"), "null")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"operation":"SQRT","a":"4","result":"2"}""", JsonCompareMode.STRICT));
    }

    @Test
    void rejectsBForUnaryOperation() throws Exception {
        expectProblem(calculate("SQRT", quoted("4"), quoted("1")), 400, "/problems/invalid-request")
                .andExpect(jsonPath("$.detail").value(containsString("'b'")))
                .andExpect(jsonPath("$.detail").value(containsString("SQRT")));
    }

    @Test
    void missingBForBinaryOperationNamesB() throws Exception {
        expectProblem(postJson("{\"operation\":\"ADD\",\"a\":\"1\"}"), 400, "/problems/invalid-request")
                .andExpect(jsonPath("$.detail").value(containsString("'b'")));
    }

    @ParameterizedTest(name = "{0} {1} = {2}")
    @CsvSource({
        "NEGATE, 5, -5",
        "NEGATE, -2.50, 2.5",
        "NEGATE, 0, 0",
        "NEGATE, -0, 0",
        "NEGATE, 0.00, 0",
        "SQRT, 4, 2",
        "SQRT, 0.25, 0.5",
        "SQRT, 2, 1.414213562373095048801688724209698",
        "SQRT, 10, 3.162277660168379331998893544432719",
        "SQRT, 1.00000000000000000000000000000000100000000000000000000000000000000025, 1",
        "SQRT, 1.00000000000000000000000000000000300000000000000000000000000000000225, 1.000000000000000000000000000000002",
        "SQRT, 0, 0",
        "SQRT, -0, 0",
        "SQRT, 0.00, 0",
    })
    void performsUnaryOperation(String operation, String a, String result) throws Exception {
        calculateUnary(operation, quoted(a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(result))
                .andExpect(jsonPath("$.b").doesNotExist());
    }

    @Test
    void unaryOperandEchoedWithItsScale() throws Exception {
        calculateUnary("NEGATE", quoted("-2.50")).andExpect(jsonPath("$.a").value("-2.50"));
    }

    @Test
    void sqrtOfLargestOperandIsExactAndPlain() throws Exception {
        calculateUnary("SQRT", quoted("1E+1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("1" + "0".repeat(500)));
    }

    @ParameterizedTest(name = "SQRT {0}")
    @CsvSource({"-1", "-0.0001"})
    void rejectsSquareRootOfANegative(String a) throws Exception {
        expectProblem(calculateUnary("SQRT", quoted(a)), 422, "/problems/negative-square-root");
    }

    @Test
    void unknownOperationWinsOverOperandCount() throws Exception {
        expectProblem(calculateUnary("NOT_AN_OPERATION", quoted("1")), 400, "/problems/unknown-operation");
    }

    @Test
    void operandCountWinsOverOperandLimits() throws Exception {
        expectProblem(calculate("SQRT", quoted("4"), quoted("1E+1001")), 400, "/problems/invalid-request");
    }

    @Test
    void operandLimitsWinOverSquareRootOfANegative() throws Exception {
        expectProblem(calculateUnary("SQRT", quoted("-1E+1001")), 400, "/problems/operand-out-of-range");
    }

    // --- OpenAPI examples (spec: Error responses documented) ---

    // ProblemExamples is copied by hand into annotations, so this is what keeps each example equal to the real body.
    @ParameterizedTest(name = "{0}")
    @MethodSource("problemExamples")
    void eachProblemExampleIsTheRealResponse(String name, String example, String body) throws Exception {
        postJson(body).andExpect(content().json(example, JsonCompareMode.STRICT));
    }

    static Object[][] problemExamples() {
        return new Object[][] {
            {"invalid-request", ProblemExamples.INVALID_REQUEST, "{\"operation\":\"ADD\",\"a\":1,"},
            {"unknown-operation", ProblemExamples.UNKNOWN_OPERATION, "{\"operation\":\"POW\",\"a\":\"2\",\"b\":\"3\"}"},
            {
                "operand-out-of-range",
                ProblemExamples.OPERAND_OUT_OF_RANGE,
                "{\"operation\":\"ADD\",\"a\":\"1E-1001\",\"b\":\"1\"}"
            },
            {"division-by-zero", ProblemExamples.DIVISION_BY_ZERO, "{\"operation\":\"DIVIDE\",\"a\":\"1\",\"b\":\"0\"}"
            },
            {"invalid-exponent", ProblemExamples.INVALID_EXPONENT, "{\"operation\":\"POWER\",\"a\":\"2\",\"b\":\"0.5\"}"
            },
            {"negative-square-root", ProblemExamples.NEGATIVE_SQUARE_ROOT, "{\"operation\":\"SQRT\",\"a\":\"-1\"}"},
            {
                "result-out-of-range",
                ProblemExamples.RESULT_OUT_OF_RANGE,
                "{\"operation\":\"POWER\",\"a\":\"100\",\"b\":\"999\"}"
            },
        };
    }

    @Test
    void frameworkErrorsAreProblemDetailsToo() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("1+2"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415));
    }

    private ResultActions expectProblem(ResultActions actions, int status, String type) throws Exception {
        return actions.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.detail").value(not(containsString("Exception"))))
                .andExpect(jsonPath("$.detail").value(not(containsString("tools.jackson"))))
                .andExpect(jsonPath("$.detail").value(not(containsString("java."))));
    }
}
