package com.paulrule.mathapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Smoke test: context starts, the service-health spec holds, and one calculation works end to end. */
@SpringBootTest
@AutoConfigureMockMvc
class ServiceHealthSmokeTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthReportsUpWithoutCredentials() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthExposesNoComponentDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void environmentEndpointIsNotExposed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    @Test
    void actuatorIndexLinksOnlyHealth() throws Exception {
        String body = mvc.perform(get("/actuator"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String, Object> links = JsonPath.read(body, "$._links");
        assertThat(links.keySet(), containsInAnyOrder("self", "health", "health-path"));
    }

    @Test
    void calculationWorksEndToEnd() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation":"MULTIPLY","a":"1.5","b":"1.5"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("2.25"));
    }
}
