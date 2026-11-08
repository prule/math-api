package com.paulrule.mathapi.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/** Response body; numbers are strings so clients never lose precision. */
@Schema(description = "The Calculation performed and its Result; every number is a string")
public record CalculationResponse(
        @Schema(example = "ADD") String operation,

        @Schema(description = "Operand a as received, plain notation", example = "0.1")
        String a,

        @Schema(description = "Operand b as received, plain notation; absent for a unary Operation", example = "0.2")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String b,

        @Schema(description = "Result, normalised (BR-4)", example = "0.3")
        String result) {}
