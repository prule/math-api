package com.paulrule.mathapi.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Request body; operands bind from JSON numbers (read exactly) or JSON strings. */
public record CalculationRequest(
        @NotNull String operation,
        @NotNull BigDecimal a,
        @NotNull BigDecimal b) {}
