package br.com.sussmartcare.presence.domain;

public enum PresenceSourceType {
  BLE,
  WIFI,
  UWB,
  QR,
  KIOSK,
  MANUAL,
  SYSTEM;

  public boolean requiresGateway() {
    return this == BLE ||
        this == WIFI ||
        this == UWB ||
        this == QR ||
        this == KIOSK;
  }
}
