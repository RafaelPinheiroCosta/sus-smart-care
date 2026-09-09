package br.com.sussmartcare.presence.adapters.in.rest;

import br.com.sussmartcare.presence.application.VisitAccessDeniedException;
import br.com.sussmartcare.presence.application.VisitAccessUnavailableException;
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

  @ExceptionHandler(VisitAccessDeniedException.class)
  ProblemDetail forbidden(
      VisitAccessDeniedException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.FORBIDDEN,
        "Acesso a visita negado",
        "visit-access-denied",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(VisitAccessUnavailableException.class)
  ProblemDetail unavailable(
      VisitAccessUnavailableException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Servico de autorizacao indisponivel",
        "visit-access-unavailable",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail badRequest(
      IllegalArgumentException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.BAD_REQUEST,
        "Regra de negocio violada",
        "business-rule",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail conflict(
      IllegalStateException exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.CONFLICT,
        "Conflito de estado",
        "conflict",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request) {

    ProblemDetail detail = problem(
        HttpStatus.BAD_REQUEST,
        "Falha de validacao",
        "validation",
        "Requisicao invalida",
        request);

    detail.setProperty(
        "errors",
        exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error ->
                error.getField()
                    + ": "
                    + error.getDefaultMessage())
            .toList());

    return detail;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(
      Exception exception,
      HttpServletRequest request) {

    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Erro interno",
        "internal-error",
        "Falha inesperada ao processar a requisicao",
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
            "urn:sus-smart-care:problem:" + type));

    detail.setInstance(
        URI.create(request.getRequestURI()));

    detail.setProperty(
        "timestamp",
        Instant.now());

    String correlationId =
        request.getHeader("X-Correlation-Id");

    detail.setProperty(
        "correlationId",
        correlationId == null
            || correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId);

    return detail;
  }
}