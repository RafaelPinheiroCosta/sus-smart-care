package br.com.sussmartcare.notification.application;

public class PatientAccessDeniedException extends RuntimeException {

  public PatientAccessDeniedException(String message) {
    super(message);
  }
}