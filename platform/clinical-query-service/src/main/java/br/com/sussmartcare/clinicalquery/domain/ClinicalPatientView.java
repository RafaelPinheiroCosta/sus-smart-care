package br.com.sussmartcare.clinicalquery.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinical_patient_views")
public class ClinicalPatientView {

  @Id
  private UUID visitId;

  private UUID patientId;

  @Column(length = 2000)
  private String preAnamnesis;

  @Column(length = 60)
  private String journeyStatus;

  @Column(length = 120)
  private String journeyStage;

  @Column(length = 40)
  private String journeyOutcome;

  private Instant journeyOccurredAt;

  private UUID transferFacilityId;

  @Column(length = 1000)
  private String terminalNote;

  private String aiRecommendation;
  private Double aiConfidence;

  @Column(length = 1200)
  private String aiReasoning;

  private String aiModelName;
  private String aiModelVersion;
  private String clinicalPriority;
  private String presenceStatus;
  private Integer queuePosition;
  private Integer estimatedMinutes;
  private String queueStatus;

  private String latestBiometricType;
  private Double latestBiometricValue;
  private String latestBiometricUnit;

  @Column(length = 120)
  private String latestAggregateType;

  private Long latestAggregateCount;
  private Double latestAggregateMinimum;
  private Double latestAggregateMaximum;
  private Double latestAggregateAverage;

  @Column(length = 40)
  private String latestAggregateUnit;

  private Instant latestAggregateMeasuredAt;

  @Column(length = 120)
  private String latestAnomalyType;

  private Double latestAnomalyValue;

  @Column(length = 40)
  private String latestAnomalyUnit;

  @Column(length = 300)
  private String latestAnomalyReason;

  private Instant latestAnomalyDetectedAt;

  private String preArrivalRisk;
  private Instant preArrivalEta;

  @Column(nullable = false)
  private Instant updatedAt;

  protected ClinicalPatientView() {
  }

  public ClinicalPatientView(
      UUID visitId) {

    this.visitId = visitId;
    this.updatedAt = Instant.now();
  }

  public void visit(
      UUID patient,
      String status) {

    patientId = patient;

    /*
     * Pre-visit/check-in events may arrive late because they belong to
     * different Kafka topics. Once a real Journey transition has been
     * projected, an intake event must never move the lifecycle backwards.
     */
    if (journeyOccurredAt == null) {
      journeyStatus = status;
    }

    touch();
  }

  public void reassignPatient(
      UUID canonicalPatientId) {

    patientId = canonicalPatientId;
    touch();
  }

  public void anamnesis(
      String text) {

    preAnamnesis = text;
    touch();
  }

  public void biometric(
      UUID patient,
      String type,
      Double value,
      String unit) {

    patientId = patient;
    latestBiometricType = type;
    latestBiometricValue = value;
    latestBiometricUnit = unit;
    touch();
  }

  public void assessment(
      String recommendation,
      Double confidence,
      String reasoning,
      String model,
      String modelVersion) {

    aiRecommendation = recommendation;
    aiConfidence = confidence;
    aiReasoning = reasoning;
    aiModelName = model;
    aiModelVersion = modelVersion;
    touch();
  }

  public void priority(
      String priority) {

    clinicalPriority = priority;
    touch();
  }

  public void presence(
      String presence) {

    presenceStatus = presence;
    touch();
  }

  public void queue(
      Integer position,
      Integer minutes,
      String status) {

    queuePosition = position;
    estimatedMinutes = minutes;
    queueStatus = status;
    touch();
  }

  public void preArrival(
      UUID patient,
      String risk,
      Instant eta) {

    patientId = patient;
    preArrivalRisk = risk;
    preArrivalEta = eta;
    touch();
  }

  public void journey(
      UUID patient,
      String status,
      String stage,
      String outcome,
      UUID targetFacilityId,
      String note,
      Instant occurredAt) {

    /*
     * Journey transitions are published to independent Kafka topics.
     * Cross-topic delivery order is not guaranteed. The read model therefore
     * accepts only the newest domain transition.
     */
    if (
        journeyOccurredAt != null &&
        occurredAt != null &&
        occurredAt.isBefore(
            journeyOccurredAt)
    ) {

      return;
    }

    if (patient != null) {
      patientId = patient;
    }

    journeyStatus = status;
    journeyStage = stage;
    journeyOutcome = outcome;
    transferFacilityId = targetFacilityId;
    terminalNote = note;

    if (occurredAt != null) {
      journeyOccurredAt = occurredAt;
    }

    touch();
  }

  public void aggregate(
      UUID patient,
      String type,
      Long count,
      Double minimum,
      Double maximum,
      Double average,
      String unit,
      Instant measuredAt) {

    if (
        latestAggregateMeasuredAt != null &&
        measuredAt != null &&
        measuredAt.isBefore(
            latestAggregateMeasuredAt)
    ) {

      return;
    }

    if (patient != null) {
      patientId = patient;
    }

    latestAggregateType = type;
    latestAggregateCount = count;
    latestAggregateMinimum = minimum;
    latestAggregateMaximum = maximum;
    latestAggregateAverage = average;
    latestAggregateUnit = unit;
    latestAggregateMeasuredAt = measuredAt;

    touch();
  }

  public void anomaly(
      UUID patient,
      String type,
      Double value,
      String unit,
      String reason,
      Instant detectedAt) {

    if (
        latestAnomalyDetectedAt != null &&
        detectedAt != null &&
        detectedAt.isBefore(
            latestAnomalyDetectedAt)
    ) {

      return;
    }

    if (patient != null) {
      patientId = patient;
    }

    latestAnomalyType = type;
    latestAnomalyValue = value;
    latestAnomalyUnit = unit;
    latestAnomalyReason = reason;
    latestAnomalyDetectedAt = detectedAt;

    touch();
  }

  private void touch() {
    updatedAt = Instant.now();
  }

  public UUID getVisitId() {
    return visitId;
  }

  public UUID getPatientId() {
    return patientId;
  }

  public String getPreAnamnesis() {
    return preAnamnesis;
  }

  public String getJourneyStatus() {
    return journeyStatus;
  }

  public String getJourneyStage() {
    return journeyStage;
  }

  public String getJourneyOutcome() {
    return journeyOutcome;
  }

  public UUID getTransferFacilityId() {
    return transferFacilityId;
  }

  public String getTerminalNote() {
    return terminalNote;
  }

  public String getAiRecommendation() {
    return aiRecommendation;
  }

  public Double getAiConfidence() {
    return aiConfidence;
  }

  public String getAiReasoning() {
    return aiReasoning;
  }

  public String getAiModelName() {
    return aiModelName;
  }

  public String getAiModelVersion() {
    return aiModelVersion;
  }

  public String getClinicalPriority() {
    return clinicalPriority;
  }

  public String getPresenceStatus() {
    return presenceStatus;
  }

  public Integer getQueuePosition() {
    return queuePosition;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public String getQueueStatus() {
    return queueStatus;
  }

  public String getLatestBiometricType() {
    return latestBiometricType;
  }

  public Double getLatestBiometricValue() {
    return latestBiometricValue;
  }

  public String getLatestBiometricUnit() {
    return latestBiometricUnit;
  }

  public String getLatestAggregateType() {
    return latestAggregateType;
  }

  public Long getLatestAggregateCount() {
    return latestAggregateCount;
  }

  public Double getLatestAggregateMinimum() {
    return latestAggregateMinimum;
  }

  public Double getLatestAggregateMaximum() {
    return latestAggregateMaximum;
  }

  public Double getLatestAggregateAverage() {
    return latestAggregateAverage;
  }

  public String getLatestAggregateUnit() {
    return latestAggregateUnit;
  }

  public Instant getLatestAggregateMeasuredAt() {
    return latestAggregateMeasuredAt;
  }

  public String getLatestAnomalyType() {
    return latestAnomalyType;
  }

  public Double getLatestAnomalyValue() {
    return latestAnomalyValue;
  }

  public String getLatestAnomalyUnit() {
    return latestAnomalyUnit;
  }

  public String getLatestAnomalyReason() {
    return latestAnomalyReason;
  }

  public Instant getLatestAnomalyDetectedAt() {
    return latestAnomalyDetectedAt;
  }

  public String getPreArrivalRisk() {
    return preArrivalRisk;
  }

  public Instant getPreArrivalEta() {
    return preArrivalEta;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
