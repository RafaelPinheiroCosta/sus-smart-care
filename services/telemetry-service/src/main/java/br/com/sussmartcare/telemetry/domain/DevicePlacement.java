package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "device_placements")
public class DevicePlacement {

  @Id
  private UUID id;

  @Column(name = "device_id", nullable = false, updatable = false)
  private UUID deviceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "placement_type", nullable = false, length = 30)
  private DevicePlacementType placementType;

  @Column(name = "facility_id")
  private UUID facilityId;

  @Column(name = "zone_id")
  private UUID zoneId;

  @Column(name = "ambulance_id", length = 100)
  private String ambulanceId;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  protected DevicePlacement() {}

  public DevicePlacement(
      UUID deviceId,
      DevicePlacementType placementType,
      UUID facilityId,
      UUID zoneId,
      String ambulanceId) {

    this.id = UUID.randomUUID();
    this.deviceId = Objects.requireNonNull(deviceId, "Device id is required");
    this.placementType =
        Objects.requireNonNull(placementType, "Placement type is required");

    validateAndAssign(facilityId, zoneId, ambulanceId);
    this.startedAt = Instant.now();
  }

  private void validateAndAssign(
      UUID facilityId,
      UUID zoneId,
      String ambulanceId) {

    switch (placementType) {
      case FACILITY -> {
        if (facilityId == null || zoneId != null || hasText(ambulanceId)) {
          throw new IllegalArgumentException("Invalid FACILITY placement");
        }
        this.facilityId = facilityId;
      }

      case CARE_ZONE -> {
        if (facilityId == null || zoneId == null || hasText(ambulanceId)) {
          throw new IllegalArgumentException("Invalid CARE_ZONE placement");
        }
        this.facilityId = facilityId;
        this.zoneId = zoneId;
      }

      case AMBULANCE -> {
        if (facilityId != null || zoneId != null || !hasText(ambulanceId)) {
          throw new IllegalArgumentException("Invalid AMBULANCE placement");
        }
        this.ambulanceId = ambulanceId.trim();
      }
    }
  }

  public void close() {
    if (!isActive()) {
      throw new IllegalStateException("Device placement is already closed");
    }
    endedAt = Instant.now();
  }

  public boolean isActive() {
    return endedAt == null;
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  public UUID getId() { return id; }
  public UUID getDeviceId() { return deviceId; }
  public DevicePlacementType getPlacementType() { return placementType; }
  public UUID getFacilityId() { return facilityId; }
  public UUID getZoneId() { return zoneId; }
  public String getAmbulanceId() { return ambulanceId; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getEndedAt() { return endedAt; }
}
