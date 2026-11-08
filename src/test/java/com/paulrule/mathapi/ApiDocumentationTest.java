package com.paulrule.mathapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.paulrule.mathapi.domain.Operation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** api-documentation spec: the generated OpenAPI document and Swagger UI. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiDocumentationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void openApiDocumentIsServedAsOpenApi31() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(startsWith("3.1")));
    }

    @Test
    void swaggerUiIsReachable() throws Exception {
        var location = mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    // --- calculation contract (spec: Calculation endpoint documented, Operands documented, Allowed operations) ---

    private static final String POST = "$.paths['/api/v1/calculations'].post";

    private DocumentContext apiDocs() throws Exception {
        return JsonPath.parse(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    /** Resolves a local {@code $ref} such as {@code #/components/schemas/X}; returns the schema unchanged otherwise. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolve(DocumentContext docs, Map<String, Object> schema) {
        var ref = (String) schema.get("$ref");
        return ref == null ? schema : docs.read("$.components.schemas." + ref.substring(ref.lastIndexOf('/') + 1));
    }

    /** Every JSON type a schema allows, whether written as type, a type array, oneOf or anyOf. */
    @SuppressWarnings("unchecked")
    private static List<String> allowedTypes(Map<String, Object> schema) {
        var types = new ArrayList<String>();
        var type = schema.get("type");
        if (type instanceof String t) types.add(t);
        if (type instanceof List<?> list) list.forEach(t -> types.add((String) t));
        for (var key : List.of("oneOf", "anyOf")) {
            if (schema.get(key) instanceof List<?> alternatives) {
                alternatives.forEach(alt -> types.addAll(allowedTypes((Map<String, Object>) alt)));
            }
        }
        return types;
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculationResponseIsJsonWithStringFields() throws Exception {
        var docs = apiDocs();
        Map<String, Object> content = docs.read(POST + ".responses['200'].content");

        assertThat(content).containsOnlyKeys("application/json");
        Map<String, Object> schema =
                resolve(docs, docs.read(POST + ".responses['200'].content['application/json'].schema"));
        Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) schema.get("properties");
        assertThat(properties).containsOnlyKeys("operation", "a", "b", "result");
        properties.values().forEach(p -> assertThat(allowedTypes(p)).containsExactly("string"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void operandsAllowNumberOrStringAndOnlyOperationAndAAreRequired() throws Exception {
        var docs = apiDocs();
        Map<String, Object> request =
                resolve(docs, docs.read(POST + ".requestBody.content['application/json'].schema"));
        var properties = (Map<String, Map<String, Object>>) request.get("properties");

        assertThat(allowedTypes(properties.get("a"))).containsExactlyInAnyOrder("number", "string");
        assertThat(allowedTypes(properties.get("b"))).containsExactlyInAnyOrder("number", "string");
        assertThat((List<String>) request.get("required")).containsExactlyInAnyOrder("operation", "a");
        // b is not required in the schema, so its description says when it is, naming the unary Operations from the
        // enum.
        var unary = Arrays.stream(Operation.values())
                .filter(op -> op.arity() == 1)
                .map(Enum::name)
                .toList();
        assertThat((String) properties.get("b").get("description"))
                .contains("binary", "unary Operation (" + String.join(", ", unary) + ")");
    }

    @Test
    @SuppressWarnings("unchecked")
    void allowedOperationsAreExactlyTheSupportedOperations() throws Exception {
        var docs = apiDocs();
        Map<String, Object> request =
                resolve(docs, docs.read(POST + ".requestBody.content['application/json'].schema"));
        var operation = ((Map<String, Map<String, Object>>) request.get("properties")).get("operation");

        assertThat((List<String>) operation.get("enum"))
                .containsExactlyElementsOf(
                        Arrays.stream(Operation.values()).map(Enum::name).toList());
    }

    // --- error responses (spec: Error responses documented, Only the real responses are documented) ---

    @Test
    void onlyTheRealResponsesAreDocumented() throws Exception {
        Map<String, Object> responses = apiDocs().read(POST + ".responses");

        assertThat(responses).containsOnlyKeys("200", "400", "422");
    }

    @Test
    void eachErrorTypeHasAProblemDetailsExample() throws Exception {
        var docs = apiDocs();

        assertThat(exampleTypes(docs, "400"))
                .containsExactlyInAnyOrder(
                        "/problems/invalid-request", "/problems/unknown-operation", "/problems/operand-out-of-range");
        assertThat(exampleTypes(docs, "422"))
                .containsExactlyInAnyOrder(
                        "/problems/division-by-zero",
                        "/problems/invalid-exponent",
                        "/problems/negative-square-root",
                        "/problems/result-out-of-range");
    }

    @Test
    void problemDetailsSchemaHasOnlyTheFieldsResponsesContain() throws Exception {
        Map<String, Object> properties = apiDocs().read("$.components.schemas.ProblemDetail.properties");

        // Spring's ProblemDetail has a 'properties' map for extensions; it is never a key in the JSON response.
        assertThat(properties).containsOnlyKeys("type", "title", "status", "detail", "instance");
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownOperationExampleListsTheSupportedOperations() throws Exception {
        Map<String, Object> example = apiDocs()
                .read(POST
                        + ".responses['400'].content['application/problem+json'].examples['unknown-operation'].value");
        var detail = (String) example.get("detail");
        var listed = detail.substring(
                detail.indexOf("(case-sensitive): ") + "(case-sensitive): ".length(), detail.length() - 1);

        // Annotation values must be constants, so this test is what keeps the example in step with the enum.
        assertThat(listed)
                .isEqualTo(String.join(
                        ", ", Arrays.stream(Operation.values()).map(Enum::name).toList()));
    }

    @SuppressWarnings("unchecked")
    private static List<String> exampleTypes(DocumentContext docs, String status) {
        Map<String, Map<String, Object>> examples =
                docs.read(POST + ".responses['" + status + "'].content['application/problem+json'].examples");
        return examples.values().stream()
                .map(example -> {
                    var value = example.get("value");
                    // Examples may be rendered as JSON objects or as JSON text.
                    Map<String, Object> body = value instanceof Map<?, ?> map
                            ? (Map<String, Object>) map
                            : JsonPath.parse((String) value).read("$");
                    return (String) body.get("type");
                })
                .toList();
    }
}
