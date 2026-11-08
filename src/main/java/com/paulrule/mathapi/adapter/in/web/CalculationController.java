package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.application.PerformCalculation;
import com.paulrule.mathapi.domain.Calculation;
import com.paulrule.mathapi.domain.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calculations")
public class CalculationController {

    private final PerformCalculation performCalculation;

    public CalculationController(PerformCalculation performCalculation) {
        this.performCalculation = performCalculation;
    }

    // Fully qualified: the swagger annotation clashes with the domain Operation enum.
    @io.swagger.v3.oas.annotations.Operation(summary = "Perform a Calculation")
    @ApiResponse(
            responseCode = "200",
            description = "The Calculation and its Result",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CalculationResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request, unknown operation, or operand out of range (BR-1)",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = {
                                @ExampleObject(name = "invalid-request", value = ProblemExamples.INVALID_REQUEST),
                                @ExampleObject(name = "unknown-operation", value = ProblemExamples.UNKNOWN_OPERATION),
                                @ExampleObject(
                                        name = "operand-out-of-range",
                                        value = ProblemExamples.OPERAND_OUT_OF_RANGE)
                            }))
    @ApiResponse(
            responseCode = "422",
            description =
                    "Division by zero (BR-2), invalid POWER exponent (BR-5), square root of a negative (BR-8), or POWER"
                            + " result out of range (BR-6)",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = {
                                @ExampleObject(name = "division-by-zero", value = ProblemExamples.DIVISION_BY_ZERO),
                                @ExampleObject(name = "invalid-exponent", value = ProblemExamples.INVALID_EXPONENT),
                                @ExampleObject(
                                        name = "negative-square-root",
                                        value = ProblemExamples.NEGATIVE_SQUARE_ROOT),
                                @ExampleObject(
                                        name = "result-out-of-range",
                                        value = ProblemExamples.RESULT_OUT_OF_RANGE)
                            }))
    @PostMapping
    public CalculationResponse calculate(@Valid @RequestBody CalculationRequest request) {
        var calculation = new Calculation(Operation.named(request.operation()), request.a(), request.b());
        var result = performCalculation.perform(calculation);
        // toPlainString keeps BR-4's plain notation (no 1E+6).
        return new CalculationResponse(
                calculation.operation().name(),
                calculation.a().toPlainString(),
                calculation.b() == null ? null : calculation.b().toPlainString(),
                result.toPlainString());
    }
}
