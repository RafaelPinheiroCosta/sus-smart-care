package br.com.sussmartcare.presence.application;

public class VisitAccessDeniedException extends RuntimeException {

  public VisitAccessDeniedException(String message) {
    super(message);
  }
}