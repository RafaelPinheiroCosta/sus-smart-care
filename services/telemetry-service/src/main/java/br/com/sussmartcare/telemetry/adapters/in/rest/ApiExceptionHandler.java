package br.com.sussmartcare.telemetry.adapters.in.rest;

import br.com.sussmartcare.telemetry.application.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(TelemetryNotFoundException.class)
  ProblemDetail notFound(
      TelemetryNotFoundException e,
      HttpServletRequest request) {
    return problem(
        HttpStatus.NOT_FOUND,
        "Resource not found",
        "not-found",
        e.getMessage(),
        request);
  }

  @ExceptionHandler({
      TelemetryConflictException.class,
      IllegalStateException.class,
      DataIntegrityViolationException.class
  })
  ProblemDetail conflict(
      Exception e,
      HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "State conflict",
        "conflict",
        e.getMessage(),
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail badRequest(
      IllegalArgumentException e,
      HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Business rule violation",
        "business-rule",
        e.getMessage(),
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(
      MethodArgumentNotValidException e,
      HttpServletRequest request) {

    ProblemDetail detail =
        problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "validation",
            "Invalid request",
            request);

    detail.setProperty(
        "errors",
        e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error ->
                error.getField() + ": " + error.getDefaultMessage())
            .toList());

    return detail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail malformed(
      HttpMessageNotReadableException e,
      HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Malformed JSON",
        "malformed-json",
        "Request body could not be parsed",
        request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(
      Exception e,
      HttpServletRequest request) {
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal error",
        "internal-error",
        "Unexpected failure while processing request",
        request);
  }

  private ProblemDetail problem(
      HttpStatus status,
      String title,
      String type,
      String detailText,
      HttpServletRequest request) {

    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(status, detailText);

    detail.setTitle(title);
    detail.setType(
        URI.create("urn:sus-smart-care:problem:" + type));
    detail.setInstance(
        URI.create(request.getRequestURI()));
    detail.setProperty("timestamp", Instant.now());

    String correlation =
        request.getHeader("X-Correlation-Id");

    detail.setProperty(
        "correlationId",
        correlation == null || correlation.isBlank()
            ? UUID.randomUUID().toString()
            : correlation);

    return detail;
  }
}
