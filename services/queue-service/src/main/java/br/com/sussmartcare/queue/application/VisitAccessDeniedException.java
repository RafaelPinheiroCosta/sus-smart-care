package br.com.sussmartcare.queue.application;

public class VisitAccessDeniedException extends RuntimeException {

  public VisitAccessDeniedException(String message) {
    super(message);
  }

  public VisitAccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }
}

