package br.com.sussmartcare.facility.adapters.in.rest;

import br.com.sussmartcare.facility.application.FacilityConflictException;
import br.com.sussmartcare.facility.application.FacilityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        "validation-error",
        "Request validation failed",
        request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail malformedJson(
      HttpMessageNotReadableException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.BAD_REQUEST,
        "Malformed JSON",
        "malformed-json",
        "Request body is invalid or malformed",
        request);
  }

  @ExceptionHandler(FacilityNotFoundException.class)
  ProblemDetail notFound(
      FacilityNotFoundException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.NOT_FOUND,
        "Resource not found",
        "not-found",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler({
      FacilityConflictException.class,
      IllegalStateException.class,
      DataIntegrityViolationException.class
  })
  ProblemDetail conflict(
      Exception exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.CONFLICT,
        "Facility state conflict",
        "conflict",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail badRequest(
      IllegalArgumentException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.BAD_REQUEST,
        "Invalid request",
        "invalid-request",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(
      HttpRequestMethodNotSupportedException.class)
  ProblemDetail methodNotAllowed(
      HttpRequestMethodNotSupportedException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.METHOD_NOT_ALLOWED,
        "Method not allowed",
        "method-not-allowed",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ProblemDetail resourceNotFound(
      NoResourceFoundException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.NOT_FOUND,
        "Resource not found",
        "not-found",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(
      Exception exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "internal-error",
        "Unexpected server error",
        request);
  }

  private ProblemDetail problem(
      HttpStatus status,
      String title,
      String code,
      String detail,
      HttpServletRequest request) {

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            status,
            detail == null ? title : detail);

    problem.setTitle(title);

    problem.setType(
        URI.create(
            "urn:sus-smart-care:error:" +
                code));

    problem.setInstance(
        URI.create(request.getRequestURI()));

    problem.setProperty(
        "timestamp",
        Instant.now());

    return problem;
  }
}
