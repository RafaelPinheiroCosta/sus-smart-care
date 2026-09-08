package br.com.sussmartcare.patientregistry.application;

public class PatientAccessDeniedException extends RuntimeException {

  public PatientAccessDeniedException(String message) {
    super(message);
  }
}