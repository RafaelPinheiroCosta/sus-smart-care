package br.com.sussmartcare.prehospital.application;

public class PreHospitalNotFoundException
    extends RuntimeException {

  public PreHospitalNotFoundException(
      String message) {

    super(message);
  }
}
