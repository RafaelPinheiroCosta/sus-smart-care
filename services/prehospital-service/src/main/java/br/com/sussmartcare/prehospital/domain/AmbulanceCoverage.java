package br.com.sussmartcare.prehospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ambulance_coverages")
public class AmbulanceCoverage {

  @Id
  @Column(
      name = "id",
      nullable = false,
      updatable = false)
  private UUID id;

  @Column(
      name = "ambulance_id",
      length = 100,
      nullable = false,
      updatable = false)
  private String ambulanceId;

  @Column(
      name = "facility_id",
      nullable = false,
      updatable = false)
  private UUID facilityId;

  @Column(
      name = "started_at",
      nullable = false,
      updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  protected AmbulanceCoverage() {
  }

  public AmbulanceCoverage(
      String ambulanceId,
      UUID facilityId) {

    this.id =
        UUID.randomUUID();

    this.ambulanceId =
        requireText(
            ambulanceId,
            "Ambulance id is required");

    this.facilityId =
        Objects.requireNonNull(
            facilityId,
            "Facility id is required");

    this.startedAt =
        Instant.now();
  }

  public void close() {

    if (!isActive()) {
      throw new IllegalStateException(
          "Ambulance coverage is already closed");
    }

    this.endedAt =
        Instant.now();
  }

  public boolean isActive() {
    return endedAt == null;
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

  public UUID getId() {
    return id;
  }

  public String getAmbulanceId() {
    return ambulanceId;
  }

  public UUID getFacilityId() {
    return facilityId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }
}
