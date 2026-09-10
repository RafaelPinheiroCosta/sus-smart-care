package br.com.sussmartcare.prehospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pre_hospital_encounters")
public class PreHospitalEncounter {

  @Id
  private UUID id;

  @Column(
      name = "patient_id",
      nullable = false)
  private UUID patientId;

  @Column(name = "visit_id")
  private UUID visitId;

  @Column(
      name = "ambulance_id",
      length = 100,
      nullable = false)
  private String ambulanceId;

  @Column(
      name = "destination_facility_id",
      nullable = false)
  private UUID destinationFacilityId;

  @Column(name = "estimated_arrival_at")
  private Instant estimatedArrivalAt;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "status",
      length = 40,
      nullable = false)
  private PreHospitalEncounterStatus status;

  @Column(
      name = "risk_level",
      length = 40)
  private String riskLevel;

  @Column(
      name = "created_at",
      nullable = false,
      updatable = false)
  private Instant createdAt;

  @Column(name = "arrived_at")
  private Instant arrivedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(
      name = "cancellation_reason",
      length = 500)
  private String cancellationReason;

  protected PreHospitalEncounter() {
  }

  public PreHospitalEncounter(
      UUID patientId,
      UUID visitId,
      String ambulanceId,
      UUID destinationFacilityId,
      Instant estimatedArrivalAt) {

    this.id =
        UUID.randomUUID();

    this.patientId =
        Objects.requireNonNull(
            patientId,
            "Patient id is required");

    this.visitId =
        visitId;

    this.ambulanceId =
        requireText(
            ambulanceId,
            "Ambulance id is required");

    this.destinationFacilityId =
        Objects.requireNonNull(
            destinationFacilityId,
            "Destination facility id is required");

    this.estimatedArrivalAt =
        estimatedArrivalAt;

    this.status =
        PreHospitalEncounterStatus.EN_ROUTE;

    this.createdAt =
        Instant.now();
  }

  public void updateEta(
      Instant estimatedArrivalAt) {

    ensureEnRoute();

    this.estimatedArrivalAt =
        Objects.requireNonNull(
            estimatedArrivalAt,
            "Estimated arrival time is required");
  }

  public void updateRisk(
      String riskLevel) {

    ensureEnRoute();

    this.riskLevel =
        requireText(
            riskLevel,
            "Risk level is required")
            .toUpperCase(Locale.ROOT);
  }

  public void markArrived() {

    ensureEnRoute();

    this.status =
        PreHospitalEncounterStatus.ARRIVED;

    this.arrivedAt =
        Instant.now();
  }

  public void cancel(
      String reason) {

    ensureEnRoute();

    this.cancellationReason =
        requireText(
            reason,
            "Cancellation reason is required");

    this.status =
        PreHospitalEncounterStatus.CANCELLED;

    this.cancelledAt =
        Instant.now();
  }

  private void ensureEnRoute() {

    if (status !=
        PreHospitalEncounterStatus.EN_ROUTE) {

      throw new IllegalStateException(
          "Pre-hospital encounter is already terminal: " +
          status);
    }
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

  public UUID getPatientId() {
    return patientId;
  }

  public UUID getVisitId() {
    return visitId;
  }

  public String getAmbulanceId() {
    return ambulanceId;
  }

  public UUID getDestinationFacilityId() {
    return destinationFacilityId;
  }

  public Instant getEstimatedArrivalAt() {
    return estimatedArrivalAt;
  }

  public PreHospitalEncounterStatus getStatus() {
    return status;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getArrivedAt() {
    return arrivedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }
}
