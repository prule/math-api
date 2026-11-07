package com.paulrule.mathapi.adapter.in.web;

/** Real ProblemDetailsAdvice response bodies, used as OpenAPI examples. Types are the stable contract. */
final class ProblemExamples {

    static final String INVALID_REQUEST = """
            {"type":"/problems/invalid-request","title":"Invalid request","status":400,\
            "detail":"Request body must be JSON with 'operation' and decimal-number operands 'a' and 'b'.",\
            "instance":"/api/v1/calculations"}""";

    static final String UNKNOWN_OPERATION = """
            {"type":"/problems/unknown-operation","title":"Unknown operation","status":400,\
            "detail":"Unknown operation 'POW'. Supported operations (case-sensitive): ADD, SUBTRACT, MULTIPLY, DIVIDE.",\
            "instance":"/api/v1/calculations"}""";

    static final String OPERAND_OUT_OF_RANGE = """
            {"type":"/problems/operand-out-of-range","title":"Operand out of range","status":400,\
            "detail":"Operand 'a' must have a scale between -1000 and 1000 (at most 1000 decimal places).",\
            "instance":"/api/v1/calculations"}""";

    static final String DIVISION_BY_ZERO = """
            {"type":"/problems/division-by-zero","title":"Division by zero","status":422,\
            "detail":"Cannot divide by zero.","instance":"/api/v1/calculations"}""";

    private ProblemExamples() {}
}
