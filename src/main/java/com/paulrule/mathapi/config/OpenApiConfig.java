package com.paulrule.mathapi.config;

import com.paulrule.mathapi.domain.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata, plus corrections springdoc can't infer: allowed operation values and the unary ones that
 * take no b, from the domain (so they
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
                        .description("Exact decimal arithmetic on one or two operands."));
    }

    @Bean
    @SuppressWarnings("unchecked")
    OpenApiCustomizer operationValuesFromDomain() {
        return openApi -> {
            var schemas = openApi.getComponents().getSchemas();
            Schema<String> operation = (Schema<String>)
                    schemas.get("CalculationRequest").getProperties().get("operation");
            operation.setEnum(Arrays.stream(Operation.values()).map(Enum::name).toList());
            Schema<?> b = (Schema<?>)
                    schemas.get("CalculationRequest").getProperties().get("b");
            var unary = Arrays.stream(Operation.values())
                    .filter(op -> op.arity() == 1)
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            b.setDescription("Required for a binary Operation; omit (or send null) for a unary Operation (" + unary
                    + "). " + b.getDescription());
            // ProblemDetail's Java 'properties' map holds extensions; responses never contain it as a key.
            schemas.get("ProblemDetail").getProperties().remove("properties");
        };
    }
}
