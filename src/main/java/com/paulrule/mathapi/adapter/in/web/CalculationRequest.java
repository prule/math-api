package com.paulrule.mathapi.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Request body; operands bind from JSON numbers (read exactly) or JSON strings. */
@Schema(description = "One Operation applied to two Operands")
public record CalculationRequest(
        // Allowed values are filled from the Operation enum by config.OpenApiConfig.
        @Schema(description = "Operation name, case-sensitive", example = "ADD") @NotNull
        String operation,

        @Schema(
                types = {"number", "string"},
                description = CalculationRequest.OPERAND_DESCRIPTION,
                example = "0.1")
        @NotNull
        BigDecimal a,

        @Schema(
                types = {"number", "string"},
                description = CalculationRequest.OPERAND_DESCRIPTION,
                example = "0.2")
        @NotNull
        BigDecimal b) {

    static final String OPERAND_DESCRIPTION =
            "Decimal number as a JSON number or string (BR-1: at most 100 significant digits, scale between -1000 and"
                    + " 1000)";
}
