package br.com.sussmartcare.prehospital.adapters.in.rest;

import br.com.sussmartcare.prehospital.application.PreHospitalConflictException;
import br.com.sussmartcare.prehospital.application.PreHospitalNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(
      PreHospitalNotFoundException.class)
  ProblemDetail notFound(
      PreHospitalNotFoundException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.NOT_FOUND,
        "Resource not found",
        "not-found",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler({
      PreHospitalConflictException.class,
      IllegalStateException.class,
      DataIntegrityViolationException.class
  })
  ProblemDetail conflict(
      Exception exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.CONFLICT,
        "State conflict",
        "conflict",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(
      IllegalArgumentException.class)
  ProblemDetail badRequest(
      IllegalArgumentException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.BAD_REQUEST,
        "Business rule violation",
        "business-rule",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(
      MethodArgumentNotValidException.class)
  ProblemDetail validation(
      MethodArgumentNotValidException exception,
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
        exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(
                error ->
                    error.getField() +
                    ": " +
                    error.getDefaultMessage())
            .toList());

    return detail;
  }

  @ExceptionHandler(
      HttpMessageNotReadableException.class)
  ProblemDetail malformedJson(
      HttpMessageNotReadableException exception,
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
      Exception exception,
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
        ProblemDetail.forStatusAndDetail(
            status,
            detailText == null
                ? status.getReasonPhrase()
                : detailText);

    detail.setTitle(title);

    detail.setType(
        URI.create(
            "urn:sus-smart-care:problem:" +
            type));

    detail.setInstance(
        URI.create(
            request.getRequestURI()));

    detail.setProperty(
        "timestamp",
        Instant.now());

    String correlationId =
        request.getHeader(
            "X-Correlation-Id");

    detail.setProperty(
        "correlationId",
        correlationId == null ||
        correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId);

    return detail;
  }
}
