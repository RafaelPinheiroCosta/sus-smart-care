package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telemetry_sessions")
public class TelemetrySession {

  @Id
  private UUID id;

  @Column(name = "patient_id", nullable = false)
  private UUID patientId;

  @Column(name = "visit_id")
  private UUID visitId;

  @Column(name = "pre_hospital_encounter_id")
  private UUID preHospitalEncounterId;

  @Column(name = "source_context", nullable = false, length = 60)
  private String sourceContext;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  protected TelemetrySession() {}

  public TelemetrySession(
      UUID patientId,
      UUID visitId,
      UUID preHospitalEncounterId,
      String sourceContext) {

    if (patientId == null) {
      throw new IllegalArgumentException("Patient id is required");
    }

    if (sourceContext == null || sourceContext.trim().isEmpty()) {
      throw new IllegalArgumentException("Source context is required");
    }

    this.id = UUID.randomUUID();
    this.patientId = patientId;
    this.visitId = visitId;
    this.preHospitalEncounterId = preHospitalEncounterId;
    this.sourceContext = sourceContext.trim().toUpperCase();
    this.startedAt = Instant.now();
  }

  public void end() {
    if (!isActive()) {
      throw new IllegalStateException("Telemetry session is already closed");
    }
    endedAt = Instant.now();
  }

  public boolean isActive() {
    return endedAt == null;
  }

  public UUID getId() { return id; }
  public UUID getPatientId() { return patientId; }
  public UUID getVisitId() { return visitId; }
  public UUID getPreHospitalEncounterId() { return preHospitalEncounterId; }
  public String getSourceContext() { return sourceContext; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getEndedAt() { return endedAt; }
}
