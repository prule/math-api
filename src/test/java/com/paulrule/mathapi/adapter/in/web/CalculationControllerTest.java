package com.paulrule.mathapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paulrule.mathapi.config.UseCaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
            })
    void rejectsInvalidRequest(String caseName, String body) throws Exception {
        expectProblem(postJson(body), 400, "/problems/invalid-request");
    }

    @ParameterizedTest
    @CsvSource({"POWER", "add", "DIVIDE"})
    void rejectsUnknownOperation(String operation) throws Exception {
        expectProblem(
                        postJson("{\"operation\":\"%s\",\"a\":\"2\",\"b\":\"3\"}".formatted(operation)),
                        400,
                        "/problems/unknown-operation")
                .andExpect(jsonPath("$.detail").value(containsString(operation)));
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
