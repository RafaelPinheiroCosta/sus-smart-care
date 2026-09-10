package br.com.sussmartcare.presence.application;

public class PresenceNotFoundException
    extends RuntimeException {

  public PresenceNotFoundException(
      String message) {

    super(message);
  }
}
