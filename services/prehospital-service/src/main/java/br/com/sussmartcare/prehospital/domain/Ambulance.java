package br.com.sussmartcare.prehospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ambulances")
public class Ambulance {

  @Id
  @Column(
      name = "id",
      length = 100,
      nullable = false,
      updatable = false)
  private String id;

  @Column(
      name = "display_name",
      length = 160,
      nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "operational_status",
      length = 40,
      nullable = false)
  private AmbulanceOperationalStatus operationalStatus;

  @Column(
      name = "created_at",
      nullable = false,
      updatable = false)
  private Instant createdAt;

  @Column(
      name = "updated_at",
      nullable = false)
  private Instant updatedAt;

  protected Ambulance() {
  }

  public Ambulance(
      String id,
      String displayName) {

    this.id =
        requireText(
            id,
            "Ambulance id is required");

    this.displayName =
        requireText(
            displayName,
            "Ambulance display name is required");

    this.operationalStatus =
        AmbulanceOperationalStatus.ACTIVE;

    this.createdAt =
        Instant.now();

    this.updatedAt =
        this.createdAt;
  }

  public void rename(String displayName) {

    this.displayName =
        requireText(
            displayName,
            "Ambulance display name is required");

    touch();
  }

  public void activate() {

    this.operationalStatus =
        AmbulanceOperationalStatus.ACTIVE;

    touch();
  }

  public void putInMaintenance() {

    this.operationalStatus =
        AmbulanceOperationalStatus.MAINTENANCE;

    touch();
  }

  public void putOutOfService() {

    this.operationalStatus =
        AmbulanceOperationalStatus.OUT_OF_SERVICE;

    touch();
  }

  public boolean isOperational() {

    return operationalStatus ==
        AmbulanceOperationalStatus.ACTIVE;
  }

  private void touch() {
    this.updatedAt = Instant.now();
  }

  private static String requireText(
      String value,
      String message) {

    Objects.requireNonNull(
        value,
        message);

    String normalized =
        value.trim();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(message);
    }

    return normalized;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public AmbulanceOperationalStatus
      getOperationalStatus() {

    return operationalStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
