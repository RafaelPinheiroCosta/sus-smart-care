package br.com.sussmartcare.presence.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "presence_gateways")
public class PresenceGateway {

  @Id
  private UUID id;

  @Column(
      name = "external_id",
      nullable = false,
      unique = true,
      length = 100)
  private String externalId;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "source_type",
      nullable = false,
      length = 20)
  private PresenceSourceType sourceType;

  @Column(
      name = "facility_id",
      nullable = false)
  private UUID facilityId;

  @Column(
      name = "zone_id",
      nullable = false,
      length = 100)
  private String zoneId;

  @Column(nullable = false)
  private boolean active;

  @Column(
      name = "created_at",
      nullable = false,
      updatable = false)
  private Instant createdAt;

  protected PresenceGateway() {
  }

  public PresenceGateway(
      String externalId,
      PresenceSourceType sourceType,
      UUID facilityId,
      String zoneId) {

    if (externalId == null ||
        externalId.trim().isEmpty()) {

      throw new IllegalArgumentException(
          "Gateway external id is required");
    }

    if (sourceType == null ||
        !sourceType.requiresGateway()) {

      throw new IllegalArgumentException(
          "Gateway must use BLE, WIFI, UWB, QR or KIOSK");
    }

    if (facilityId == null) {
      throw new IllegalArgumentException(
          "Facility id is required");
    }

    if (zoneId == null ||
        zoneId.trim().isEmpty()) {

      throw new IllegalArgumentException(
          "Zone id is required");
    }

    this.id = UUID.randomUUID();
    this.externalId = externalId.trim();
    this.sourceType = sourceType;
    this.facilityId = facilityId;
    this.zoneId = zoneId.trim();
    this.active = true;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getExternalId() {
    return externalId;
  }

  public PresenceSourceType getSourceType() {
    return sourceType;
  }

  public UUID getFacilityId() {
    return facilityId;
  }

  public String getZoneId() {
    return zoneId;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
