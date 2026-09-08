package br.com.sussmartcare.triage.adapters.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail badRequest(IllegalArgumentException exception, HttpServletRequest request) {
    return problem(HttpStatus.BAD_REQUEST, "Regra de negócio violada", "business-rule", exception.getMessage(), request);
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail conflict(IllegalStateException exception, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, "Conflito de estado", "conflict", exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
    ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Falha de validação", "validation", "Requisição inválida", request);
    detail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList());
    return detail;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "internal-error",
        "Falha inesperada ao processar a requisição", request);
  }

  private ProblemDetail problem(
      HttpStatus status, String title, String type, String detailText, HttpServletRequest request) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, detailText == null ? status.getReasonPhrase() : detailText);
    detail.setTitle(title);
    detail.setType(URI.create("urn:sus-smart-care:problem:" + type));
    detail.setInstance(URI.create(request.getRequestURI()));
    detail.setProperty("timestamp", Instant.now());
    String correlationId = request.getHeader("X-Correlation-Id");
    detail.setProperty("correlationId", correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId);
    return detail;
  }
}
