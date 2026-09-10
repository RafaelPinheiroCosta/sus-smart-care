package br.com.sussmartcare.presence.adapters.in.rest;

import br.com.sussmartcare.presence.application.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(
      PresenceNotFoundException.class)
  ProblemDetail notFound(
      PresenceNotFoundException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.NOT_FOUND,
        "Resource not found",
        "not-found",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler({
      PresenceConflictException.class,
      IllegalStateException.class
  })
  ProblemDetail conflict(
      RuntimeException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.CONFLICT,
        "Presence conflict",
        "presence-conflict",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(
      VisitAccessDeniedException.class)
  ProblemDetail forbidden(
      VisitAccessDeniedException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.FORBIDDEN,
        "Visit access denied",
        "visit-access-denied",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(
      VisitAccessUnavailableException.class)
  ProblemDetail unavailable(
      VisitAccessUnavailableException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Authorization service unavailable",
        "visit-access-unavailable",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler({
      IllegalArgumentException.class,
      MethodArgumentNotValidException.class,
      HttpMessageNotReadableException.class
  })
  ProblemDetail badRequest(
      Exception exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.BAD_REQUEST,
        "Invalid request",
        "invalid-request",
        exception.getMessage(),
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

    detail.setTitle(
        title);

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
            ? UUID.randomUUID()
                .toString()
            : correlationId);

    return detail;
  }
}
