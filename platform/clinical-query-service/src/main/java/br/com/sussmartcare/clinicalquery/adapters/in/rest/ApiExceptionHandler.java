package br.com.sussmartcare.clinicalquery.adapters.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail notFound(IllegalArgumentException exception, HttpServletRequest request) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    detail.setTitle("Clinical view not found");
    detail.setType(URI.create("urn:sus-smart-care:problem:clinical-view-not-found"));
    detail.setInstance(URI.create(request.getRequestURI()));
    detail.setProperty("timestamp", Instant.now());
    detail.setProperty("correlationId", request.getHeader("X-Correlation-Id"));
    return detail;
  }
}
