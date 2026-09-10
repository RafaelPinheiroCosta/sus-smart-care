package br.com.sussmartcare.presence.application;

public class PresenceConflictException
    extends RuntimeException {

  public PresenceConflictException(
      String message) {

    super(message);
  }
}
