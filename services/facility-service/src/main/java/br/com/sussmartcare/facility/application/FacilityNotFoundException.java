package br.com.sussmartcare.facility.application;

public class FacilityNotFoundException extends RuntimeException {

  public FacilityNotFoundException(String message) {
    super(message);
  }
}
