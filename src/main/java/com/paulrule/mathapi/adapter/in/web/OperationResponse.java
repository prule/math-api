package com.paulrule.mathapi.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** One supported Operation; arity is a count, not a decimal value, so it stays a JSON integer. */
@Schema(description = "A supported Operation and its arity")
public record OperationResponse(
        @Schema(description = "The case-sensitive value to send as 'operation'", example = "ADD")
        String name,

        @Schema(description = "Number of Operands: 1 (a only) or 2 (a and b) (BR-9)", example = "2")
        int arity) {}
