package com.paulrule.mathapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** api-documentation spec: docs are disabled under the prod profile; nothing else changes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ApiDocumentationDisabledTest {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @ValueSource(
            strings = {"/v3/api-docs", "/swagger-ui.html", "/swagger-ui/index.html", "/webjars/swagger-ui/index.html"})
    void docsEndpointsAreNotFound(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void calculationApiIsUnaffected() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation":"MULTIPLY","a":"1.5","b":"1.5"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("2.25"));
    }

    @Test
    void healthIsUnaffected() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
