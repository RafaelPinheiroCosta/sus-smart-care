package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "medical_devices")
public class MedicalDevice {

  @Id
  private UUID id;

  @Column(name = "external_id", nullable = false, unique = true, length = 120)
  private String externalId;

  @Column(name = "device_type", nullable = false, length = 120)
  private String deviceType;

  @Column(length = 120)
  private String manufacturer;

  @Column(length = 120)
  private String model;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DeviceStatus status;

  @Column(name = "registered_at", nullable = false, updatable = false)
  private Instant registeredAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MedicalDevice() {}

  public MedicalDevice(
      String externalId,
      String deviceType,
      String manufacturer,
      String model) {

    this.id = UUID.randomUUID();
    this.externalId = requireText(externalId, "External id is required");
    this.deviceType = requireText(deviceType, "Device type is required");
    this.manufacturer = normalizeNullable(manufacturer);
    this.model = normalizeNullable(model);
    this.status = DeviceStatus.ACTIVE;
    this.registeredAt = Instant.now();
    this.updatedAt = this.registeredAt;
  }

  public void changeStatus(DeviceStatus target) {

    Objects.requireNonNull(target, "Device status is required");

    if (status == DeviceStatus.REVOKED &&
        target != DeviceStatus.REVOKED) {
      throw new IllegalStateException(
          "Revoked device cannot be reactivated");
    }

    status = target;
    updatedAt = Instant.now();
  }

  public boolean isOperational() {
    return status == DeviceStatus.ACTIVE;
  }

  private static String requireText(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  private static String normalizeNullable(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return value.trim();
  }

  public UUID getId() { return id; }
  public String getExternalId() { return externalId; }
  public String getDeviceType() { return deviceType; }
  public String getManufacturer() { return manufacturer; }
  public String getModel() { return model; }
  public DeviceStatus getStatus() { return status; }
  public Instant getRegisteredAt() { return registeredAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
