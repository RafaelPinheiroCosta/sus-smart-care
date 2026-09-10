package br.com.sussmartcare.facility.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "beds")
public class Bed {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID zoneId;

  @Column(nullable = false, length = 80)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BedType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BedOperationalStatus operationalStatus;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected Bed() {}

  public Bed(
      UUID zoneId,
      String code,
      BedType type) {

    this.id = UUID.randomUUID();
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    this.code = requireText(code, "code");
    this.type = Objects.requireNonNull(type, "type");
    this.operationalStatus = BedOperationalStatus.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void putInMaintenance() {
    this.operationalStatus = BedOperationalStatus.MAINTENANCE;
    touch();
  }

  public void putOutOfService() {
    this.operationalStatus = BedOperationalStatus.OUT_OF_SERVICE;
    touch();
  }

  public void activate() {
    this.operationalStatus = BedOperationalStatus.ACTIVE;
    touch();
  }

  public boolean isOperational() {
    return operationalStatus == BedOperationalStatus.ACTIVE;
  }

  private void touch() {
    this.updatedAt = Instant.now();
  }

  private static String requireText(String value, String field) {

    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }

    return value.trim();
  }

  public UUID getId() {
    return id;
  }

  public UUID getZoneId() {
    return zoneId;
  }

  public String getCode() {
    return code;
  }

  public BedType getType() {
    return type;
  }

  public BedOperationalStatus getOperationalStatus() {
    return operationalStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
