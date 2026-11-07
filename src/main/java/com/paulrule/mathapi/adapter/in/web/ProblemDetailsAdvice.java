package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.domain.DivisionByZeroException;
import com.paulrule.mathapi.domain.Operation;
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

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem =
                invalidRequest("Request body must be JSON with 'operation' and decimal-number operands 'a' and 'b'.");
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

    @ExceptionHandler(DivisionByZeroException.class)
    ProblemDetail handleDivisionByZero(DivisionByZeroException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Cannot divide by zero.");
        problem.setType(DIVISION_BY_ZERO);
        problem.setTitle("Division by zero");
        return problem;
    }

    private static ProblemDetail invalidRequest(String detail) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(INVALID_REQUEST);
        problem.setTitle("Invalid request");
        return problem;
    }
}
