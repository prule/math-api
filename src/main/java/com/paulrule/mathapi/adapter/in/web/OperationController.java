package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.domain.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {

    // Fully qualified: the swagger annotation clashes with the domain Operation enum.
    @io.swagger.v3.oas.annotations.Operation(summary = "List the supported Operations and their arity")
    @ApiResponse(
            responseCode = "200",
            description = "Every supported Operation, in the order the service declares them",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OperationResponse.class))))
    @GetMapping
    public List<OperationResponse> list() {
        return Arrays.stream(Operation.values())
                .map(op -> new OperationResponse(op.name(), op.arity()))
                .toList();
    }
}
