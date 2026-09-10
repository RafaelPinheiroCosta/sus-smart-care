package br.com.sussmartcare.telemetry.application;

public class TelemetryConflictException extends RuntimeException {
  public TelemetryConflictException(String message) {
    super(message);
  }
}
