package com.paulrule.mathapi.application;

import com.paulrule.mathapi.domain.Calculation;
import java.math.BigDecimal;

/** Use case: perform one Calculation and return its Result. */
public class PerformCalculation {

    public BigDecimal perform(Calculation calculation) {
        return calculation.result();
    }
}
