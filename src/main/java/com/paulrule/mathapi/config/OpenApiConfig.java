package com.paulrule.mathapi.config;

import com.paulrule.mathapi.domain.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata, plus corrections springdoc can't infer: allowed operation values from the domain (so they
 * never drift) and the Problem Details schema as it is actually serialised.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI mathApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Math API")
                        .version("v1")
                        .description("Exact decimal arithmetic on two operands."));
    }

    @Bean
    @SuppressWarnings("unchecked")
    OpenApiCustomizer operationValuesFromDomain() {
        return openApi -> {
            var schemas = openApi.getComponents().getSchemas();
            Schema<String> operation = (Schema<String>)
                    schemas.get("CalculationRequest").getProperties().get("operation");
            operation.setEnum(Arrays.stream(Operation.values()).map(Enum::name).toList());
            // ProblemDetail's Java 'properties' map holds extensions; responses never contain it as a key.
            schemas.get("ProblemDetail").getProperties().remove("properties");
        };
    }
}
