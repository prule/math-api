package com.paulrule.mathapi.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.paulrule.mathapi.config.UseCaseConfig;
import com.paulrule.mathapi.domain.Operation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** operation-discovery spec. CalculationController is loaded to prove listed names are accepted. */
@WebMvcTest({OperationController.class, CalculationController.class})
@Import(UseCaseConfig.class)
class OperationControllerTest {

    @Autowired
    MockMvc mvc;

    private List<Map<String, Object>> listed() throws Exception {
        var body = mvc.perform(get("/api/v1/operations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(body).read("$");
    }

    @Test
    void listsEverySupportedOperationInOrderWithItsArity() throws Exception {
        var expected = Arrays.stream(Operation.values())
                .map(op -> Map.<String, Object>of("name", op.name(), "arity", op.arity()))
                .toList();

        assertThat(listed()).containsExactlyElementsOf(expected);
    }

    @Test
    void binaryAndUnaryEntriesGiveNameAndIntegerArity() throws Exception {
        // BR-9; contains (not containsExactly) so adding an Operation never breaks this test.
        assertThat(listed())
                .contains(
                        Map.of("name", "ADD", "arity", 2),
                        Map.of("name", "SQRT", "arity", 1),
                        Map.of("name", "NEGATE", "arity", 1));
    }

    @Test
    void orderMatchesTheUnknownOperationDetail() throws Exception {
        var detail = JsonPath.<String>read(
                mvc.perform(post("/api/v1/calculations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operation\":\"NOPE\",\"a\":\"1\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.detail");
        var names = listed().stream().map(entry -> (String) entry.get("name")).toList();

        assertThat(detail).endsWith("(case-sensitive): " + String.join(", ", names) + ".");
    }

    @Test
    void everyListedNameIsAcceptedByTheCalculationEndpoint() throws Exception {
        for (var entry : listed()) {
            var b = ((Integer) entry.get("arity")) == 2 ? ",\"b\":\"2\"" : "";
            mvc.perform(post("/api/v1/calculations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"operation\":\"" + entry.get("name") + "\",\"a\":\"4\"" + b + "}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void headIsServed() throws Exception {
        mvc.perform(head("/api/v1/operations")).andExpect(status().isOk());
    }

    @Test
    void otherMethodsGetProblemDetails405() throws Exception {
        mvc.perform(post("/api/v1/operations"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
