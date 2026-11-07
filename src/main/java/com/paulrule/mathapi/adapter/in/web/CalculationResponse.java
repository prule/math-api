package com.paulrule.mathapi.adapter.in.web;

/** Response body; numbers are strings so clients never lose precision. */
public record CalculationResponse(String operation, String a, String b, String result) {}
