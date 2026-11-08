package com.paulrule.mathapi.adapter.in.web;

/** Real ProblemDetailsAdvice response bodies, used as OpenAPI examples. Types are the stable contract. */
final class ProblemExamples {

    static final String INVALID_REQUEST = """
            {"type":"/problems/invalid-request","title":"Invalid request","status":400,\
            "detail":"Request body must be JSON with 'operation' and decimal-number operand 'a' (and 'b' for a binary operation).",\
            "instance":"/api/v1/calculations"}""";

    static final String UNKNOWN_OPERATION = """
            {"type":"/problems/unknown-operation","title":"Unknown operation","status":400,\
            "detail":"Unknown operation 'POW'. Supported operations (case-sensitive): ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, MODULO, SQRT, NEGATE.",\
            "instance":"/api/v1/calculations"}""";

    static final String OPERAND_OUT_OF_RANGE = """
            {"type":"/problems/operand-out-of-range","title":"Operand out of range","status":400,\
            "detail":"Operand 'a' must have a scale between -1000 and 1000 (at most 1000 decimal places).",\
            "instance":"/api/v1/calculations"}""";

    static final String DIVISION_BY_ZERO = """
            {"type":"/problems/division-by-zero","title":"Division by zero","status":422,\
            "detail":"Cannot divide by zero.","instance":"/api/v1/calculations"}""";

    static final String INVALID_EXPONENT = """
            {"type":"/problems/invalid-exponent","title":"Invalid exponent","status":422,\
            "detail":"POWER needs an integer exponent between -999 and 999.","instance":"/api/v1/calculations"}""";

    static final String NEGATIVE_SQUARE_ROOT = """
            {"type":"/problems/negative-square-root","title":"Negative square root","status":422,\
            "detail":"SQRT needs a non-negative operand.","instance":"/api/v1/calculations"}""";

    static final String RESULT_OUT_OF_RANGE = """
            {"type":"/problems/result-out-of-range","title":"Result out of range","status":422,\
            "detail":"The result's scale must be between -1000 and 1000.","instance":"/api/v1/calculations"}""";

    private ProblemExamples() {}
}
