package br.com.sussmartcare.notification.application;

public class PatientAccessUnavailableException extends RuntimeException {

  public PatientAccessUnavailableException(
      String message,
      Throwable cause) {

    super(message, cause);
  }
}