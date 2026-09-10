package br.com.sussmartcare.clinicalquery.application;

import br.com.sussmartcare.clinicalquery.domain.ClinicalLatestObservationView;
import br.com.sussmartcare.clinicalquery.domain.ClinicalPatientView;
import br.com.sussmartcare.clinicalquery.domain.PatientProfileView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Eventually consistent read model optimized for the professional current-care
 * screen. Command-side bounded contexts remain authoritative.
 */
public record ClinicalPatientViewSnapshot(
    UUID visitId,
    UUID patientId,
    String patientName,
    LocalDate birthDate,
    String identityStatus,
    String preAnamnesis,
    String journeyStatus,
    String journeyStage,
    String journeyOutcome,
    UUID transferFacilityId,
    String terminalNote,
    String aiRecommendation,
    Double aiConfidence,
    String aiReasoning,
    String aiModelName,
    String aiModelVersion,
    String clinicalPriority,
    String presenceStatus,
    Integer queuePosition,
    Integer estimatedMinutes,
    String queueStatus,
    List<VitalSnapshot> latestVitals,
    ContinuousMetricSnapshot continuousTelemetry,
    AnomalySnapshot latestAnomaly,
    String preArrivalRisk,
    Instant preArrivalEta,
    Instant updatedAt) {

  public static ClinicalPatientViewSnapshot from(
      ClinicalPatientView view,
      PatientProfileView profile,
      List<ClinicalLatestObservationView> observations) {

    var vitals =
        observations.stream()
            .map(VitalSnapshot::from)
            .toList();

    ContinuousMetricSnapshot aggregate =
        view.getLatestAggregateType() == null
            ? null
            : new ContinuousMetricSnapshot(
                view.getLatestAggregateType(),
                view.getLatestAggregateCount(),
                view.getLatestAggregateMinimum(),
                view.getLatestAggregateMaximum(),
                view.getLatestAggregateAverage(),
                view.getLatestAggregateUnit(),
                view.getLatestAggregateMeasuredAt());

    AnomalySnapshot anomaly =
        view.getLatestAnomalyType() == null
            ? null
            : new AnomalySnapshot(
                view.getLatestAnomalyType(),
                view.getLatestAnomalyValue(),
                view.getLatestAnomalyUnit(),
                view.getLatestAnomalyReason(),
                view.getLatestAnomalyDetectedAt());

    return new ClinicalPatientViewSnapshot(
        view.getVisitId(),
        view.getPatientId(),
        profile == null
            ? null
            : profile.getFullName(),
        profile == null
            ? null
            : profile.getBirthDate(),
        profile == null
            ? null
            : profile.getIdentityStatus(),
        view.getPreAnamnesis(),
        view.getJourneyStatus(),
        view.getJourneyStage(),
        view.getJourneyOutcome(),
        view.getTransferFacilityId(),
        view.getTerminalNote(),
        view.getAiRecommendation(),
        view.getAiConfidence(),
        view.getAiReasoning(),
        view.getAiModelName(),
        view.getAiModelVersion(),
        view.getClinicalPriority(),
        view.getPresenceStatus(),
        view.getQueuePosition(),
        view.getEstimatedMinutes(),
        view.getQueueStatus(),
        vitals,
        aggregate,
        anomaly,
        view.getPreArrivalRisk(),
        view.getPreArrivalEta(),
        view.getUpdatedAt());
  }

  public record VitalSnapshot(
      String type,
      Double value,
      String unit,
      Instant measuredAt) {

    static VitalSnapshot from(
        ClinicalLatestObservationView observation) {

      return new VitalSnapshot(
          observation.getType(),
          observation.getValue(),
          observation.getUnit(),
          observation.getMeasuredAt());
    }
  }

  public record ContinuousMetricSnapshot(
      String type,
      Long count,
      Double minimum,
      Double maximum,
      Double average,
      String unit,
      Instant lastMeasuredAt) {
  }

  public record AnomalySnapshot(
      String type,
      Double value,
      String unit,
      String reason,
      Instant detectedAt) {
  }
}
