package br.com.sussmartcare.patientjourney.application;

public class PatientAccessDeniedException extends RuntimeException {

  public PatientAccessDeniedException(String message) {
    super(message);
  }
}