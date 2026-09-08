package br.com.sussmartcare.clinicalquery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-model projection containing the most recent measurement of one biometric
 * type for a visit. It is deliberately a projection, not a clinical source of truth.
 */
@Entity
@Table(
    name = "clinical_latest_observation_views",
    uniqueConstraints = @UniqueConstraint(name = "uk_clinical_latest_observation", columnNames = {"visit_id", "type"}))
public class ClinicalLatestObservationView {

  @Id private UUID id;

  @Column(name = "visit_id", nullable = false)
  private UUID visitId;

  @Column(nullable = false, length = 120)
  private String type;

  @Column(nullable = false)
  private Double value;

  @Column(length = 40)
  private String unit;

  @Column(name = "measured_at", nullable = false)
  private Instant measuredAt;

  protected ClinicalLatestObservationView() {}

  public ClinicalLatestObservationView(
      UUID visitId, String type, Double value, String unit, Instant measuredAt) {
    this.id = UUID.randomUUID();
    this.visitId = visitId;
    this.type = type;
    this.value = value;
    this.unit = unit;
    this.measuredAt = measuredAt;
  }

  public void updateIfNewer(Double value, String unit, Instant measuredAt) {
    if (measuredAt == null) {
      return;
    }
    if (this.measuredAt == null || !measuredAt.isBefore(this.measuredAt)) {
      this.value = value;
      this.unit = unit;
      this.measuredAt = measuredAt;
    }
  }

  public UUID getVisitId() { return visitId; }
  public String getType() { return type; }
  public Double getValue() { return value; }
  public String getUnit() { return unit; }
  public Instant getMeasuredAt() { return measuredAt; }
}
