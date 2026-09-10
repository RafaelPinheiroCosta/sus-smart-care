package br.com.sussmartcare.telemetry.application;

public class TelemetryNotFoundException extends RuntimeException {
  public TelemetryNotFoundException(String message) {
    super(message);
  }
}
