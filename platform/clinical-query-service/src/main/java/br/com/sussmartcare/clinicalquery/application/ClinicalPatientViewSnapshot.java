package br.com.sussmartcare.clinicalquery.application;

import br.com.sussmartcare.clinicalquery.domain.ClinicalLatestObservationView;
import br.com.sussmartcare.clinicalquery.domain.ClinicalPatientView;
import br.com.sussmartcare.clinicalquery.domain.PatientProfileView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * View Data optimized for the professional's current-care screen.
 * It is eventually consistent and does not replace the source records in each bounded context.
 */
public record ClinicalPatientViewSnapshot(
    UUID visitId,
    UUID patientId,
    String patientName,
    LocalDate birthDate,
    String identityStatus,
    String preAnamnesis,
    String journeyStatus,
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
    String preArrivalRisk,
    Instant preArrivalEta,
    Instant updatedAt) {

  public static ClinicalPatientViewSnapshot from(
      ClinicalPatientView view,
      PatientProfileView profile,
      List<ClinicalLatestObservationView> observations) {
    var vitals = observations.stream()
        .map(VitalSnapshot::from)
        .toList();
    return new ClinicalPatientViewSnapshot(
        view.getVisitId(),
        view.getPatientId(),
        profile == null ? null : profile.getFullName(),
        profile == null ? null : profile.getBirthDate(),
        profile == null ? null : profile.getIdentityStatus(),
        view.getPreAnamnesis(),
        view.getJourneyStatus(),
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
        view.getPreArrivalRisk(),
        view.getPreArrivalEta(),
        view.getUpdatedAt());
  }

  public record VitalSnapshot(String type, Double value, String unit, Instant measuredAt) {
    static VitalSnapshot from(ClinicalLatestObservationView observation) {
      return new VitalSnapshot(
          observation.getType(),
          observation.getValue(),
          observation.getUnit(),
          observation.getMeasuredAt());
    }
  }
}
