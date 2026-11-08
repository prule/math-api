package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.domain.ArityMismatchException;
import com.paulrule.mathapi.domain.Calculation;
import com.paulrule.mathapi.domain.DivisionByZeroException;
import com.paulrule.mathapi.domain.InvalidExponentException;
import com.paulrule.mathapi.domain.NegativeSquareRootException;
import com.paulrule.mathapi.domain.OperandOutOfRangeException;
import com.paulrule.mathapi.domain.Operation;
import com.paulrule.mathapi.domain.ResultOutOfRangeException;
import com.paulrule.mathapi.domain.UnknownOperationException;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every error to RFC 9457 Problem Details. Extending ResponseEntityExceptionHandler also covers framework errors
 * (405, 406, 415). Details are written here, never copied from exceptions, so no internals leak.
 */
@RestControllerAdvice
public class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

    static final URI INVALID_REQUEST = URI.create("/problems/invalid-request");
    static final URI UNKNOWN_OPERATION = URI.create("/problems/unknown-operation");
    static final URI DIVISION_BY_ZERO = URI.create("/problems/division-by-zero");
    static final URI OPERAND_OUT_OF_RANGE = URI.create("/problems/operand-out-of-range");
    static final URI INVALID_EXPONENT = URI.create("/problems/invalid-exponent");
    static final URI RESULT_OUT_OF_RANGE = URI.create("/problems/result-out-of-range");
    static final URI NEGATIVE_SQUARE_ROOT = URI.create("/problems/negative-square-root");

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = invalidRequest(
                "Request body must be JSON with 'operation' and decimal-number operand 'a' (and 'b' for a binary operation).");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> "'" + error.getField() + "'")
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        var problem = invalidRequest("Missing required field(s): " + fields + ".");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(UnknownOperationException.class)
    ProblemDetail handleUnknownOperation(UnknownOperationException ex) {
        var supported = Arrays.stream(Operation.values()).map(Enum::name).collect(Collectors.joining(", "));
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Unknown operation '" + ex.getName() + "'. Supported operations (case-sensitive): " + supported + ".");
        problem.setType(UNKNOWN_OPERATION);
        problem.setTitle("Unknown operation");
        return problem;
    }

    @ExceptionHandler(ArityMismatchException.class)
    ProblemDetail handleArityMismatch(ArityMismatchException ex) {
        var operation = ex.operation();
        return invalidRequest(
                operation.arity() == 2
                        ? "Missing required field(s): 'b'."
                        : "'b' must be omitted for " + operation.name() + ", which takes one operand.");
    }

    @ExceptionHandler(NegativeSquareRootException.class)
    ProblemDetail handleNegativeSquareRoot(NegativeSquareRootException ex) {
        return unprocessable(NEGATIVE_SQUARE_ROOT, "Negative square root", "SQRT needs a non-negative operand.");
    }

    @ExceptionHandler(DivisionByZeroException.class)
    ProblemDetail handleDivisionByZero(DivisionByZeroException ex) {
        return unprocessable(DIVISION_BY_ZERO, "Division by zero", "Cannot divide by zero.");
    }

    @ExceptionHandler(InvalidExponentException.class)
    ProblemDetail handleInvalidExponent(InvalidExponentException ex) {
        return unprocessable(
                INVALID_EXPONENT, "Invalid exponent", "POWER needs an integer exponent between -999 and 999.");
    }

    @ExceptionHandler(ResultOutOfRangeException.class)
    ProblemDetail handleResultOutOfRange(ResultOutOfRangeException ex) {
        return unprocessable(
                RESULT_OUT_OF_RANGE, "Result out of range", "The result's scale must be between -1000 and 1000.");
    }

    @ExceptionHandler(OperandOutOfRangeException.class)
    ProblemDetail handleOperandOutOfRange(OperandOutOfRangeException ex) {
        var rule = switch (ex.limit()) {
            case DIGITS -> "must have at most " + Calculation.MAX_DIGITS + " significant digits";
            case SCALE ->
                "must have a scale between -" + Calculation.MAX_SCALE + " and " + Calculation.MAX_SCALE + " (at most "
                        + Calculation.MAX_SCALE + " decimal places)";
        };
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Operand '" + ex.operand() + "' " + rule + ".");
        problem.setType(OPERAND_OUT_OF_RANGE);
        problem.setTitle("Operand out of range");
        return problem;
    }

    private static ProblemDetail unprocessable(URI type, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, detail);
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }

    private static ProblemDetail invalidRequest(String detail) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(INVALID_REQUEST);
        problem.setTitle("Invalid request");
        return problem;
    }
}
