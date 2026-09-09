package br.com.sussmartcare.presence.application;

public class VisitAccessUnavailableException extends RuntimeException {

  public VisitAccessUnavailableException(
      String message,
      Throwable cause) {

    super(message, cause);
  }
}