package br.com.sussmartcare.prehospital.application;

public class PreHospitalConflictException
    extends RuntimeException {

  public PreHospitalConflictException(
      String message) {

    super(message);
  }
}
