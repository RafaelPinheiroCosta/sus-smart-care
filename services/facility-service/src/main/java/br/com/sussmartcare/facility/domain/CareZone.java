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
@Table(name = "care_zones")
public class CareZone {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID facilityId;

  @Column(nullable = false, length = 80)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CareZoneType type;

  @Column(nullable = false)
  private boolean active;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CareZone() {}

  public CareZone(
      UUID facilityId,
      String code,
      String name,
      CareZoneType type) {

    this.id = UUID.randomUUID();
    this.facilityId =
        Objects.requireNonNull(facilityId, "facilityId");
    this.code = requireText(code, "code");
    this.name = requireText(name, "name");
    this.type = Objects.requireNonNull(type, "type");
    this.active = true;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void rename(String name) {
    this.name = requireText(name, "name");
    touch();
  }

  public void activate() {
    this.active = true;
    touch();
  }

  public void deactivate() {
    this.active = false;
    touch();
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

  public UUID getFacilityId() {
    return facilityId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public CareZoneType getType() {
    return type;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
