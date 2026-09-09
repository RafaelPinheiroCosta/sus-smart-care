package br.com.sussmartcare.queue.application;

public class VisitAccessUnavailableException extends RuntimeException {

  public VisitAccessUnavailableException(String message) {
    super(message);
  }

  public VisitAccessUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}

