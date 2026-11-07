package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.application.PerformCalculation;
import com.paulrule.mathapi.domain.Calculation;
import com.paulrule.mathapi.domain.Operation;
import jakarta.validation.Valid;
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

    @PostMapping
    public CalculationResponse calculate(@Valid @RequestBody CalculationRequest request) {
        var calculation = new Calculation(Operation.named(request.operation()), request.a(), request.b());
        var result = performCalculation.perform(calculation);
        // toPlainString keeps BR-4's plain notation (no 1E+6).
        return new CalculationResponse(
                calculation.operation().name(),
                calculation.a().toPlainString(),
                calculation.b().toPlainString(),
                result.toPlainString());
    }
}
