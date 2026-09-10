package br.com.sussmartcare.prehospital.application;

import br.com.sussmartcare.prehospital.domain.Ambulance;
import br.com.sussmartcare.prehospital.domain.PreHospitalEncounter;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceCoverageRepository;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceRepository;
import br.com.sussmartcare.prehospital.infrastructure.PreHospitalRepository;
import br.com.sussmartcare.prehospital.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreHospitalApplicationService {

  private final PreHospitalRepository repo;
  private final EventOutbox outbox;
  private final AmbulanceRepository ambulanceRepository;
  private final AmbulanceCoverageRepository coverageRepository;

  public PreHospitalApplicationService(
      PreHospitalRepository repo,
      EventOutbox outbox,
      AmbulanceRepository ambulanceRepository,
      AmbulanceCoverageRepository coverageRepository) {

    this.repo = repo;
    this.outbox = outbox;
    this.ambulanceRepository = ambulanceRepository;
    this.coverageRepository = coverageRepository;
  }

  @Transactional
  public PreHospitalEncounter create(
      UUID patient,
      UUID visit,
      String ambulanceId,
      UUID destination,
      Instant eta) {

    Ambulance ambulance =
        requireOperationalAmbulance(
            ambulanceId);

    requireActiveCoverage(
        ambulance.getId());

    PreHospitalEncounter encounter =
        repo.save(
            new PreHospitalEncounter(
                patient,
                visit,
                ambulance.getId(),
                destination,
                eta));

    outbox.append(
        "pre-hospital-encounter-created",
        encounter.getId().toString(),
        new EncounterCreated(
            encounter.getId(),
            patient,
            visit,
            ambulance.getId(),
            destination,
            eta));

    return encounter;
  }

  @Transactional(readOnly = true)
  public PreHospitalEncounter get(
      UUID id) {

    return repo.findById(id)
        .orElseThrow(
            () ->
                new PreHospitalNotFoundException(
                    "Pre-hospital encounter not found: " +
                    id));
  }

  @Transactional
  public PreHospitalEncounter eta(
      UUID id,
      Instant eta) {

    PreHospitalEncounter encounter =
        get(id);

    encounter.updateEta(eta);

    return repo.save(encounter);
  }

  @Transactional
  public PreHospitalEncounter risk(
      UUID id,
      String risk) {

    PreHospitalEncounter encounter =
        get(id);

    encounter.updateRisk(risk);

    repo.save(encounter);

    if ("HIGH".equals(encounter.getRiskLevel()) ||
        "EMERGENCY".equals(encounter.getRiskLevel())) {

      outbox.append(
          "pre-arrival-alert-created",
          id.toString(),
          new PreArrivalAlert(
              id,
              encounter.getPatientId(),
              encounter.getVisitId(),
              encounter.getDestinationFacilityId(),
              encounter.getRiskLevel(),
              encounter.getEstimatedArrivalAt()));
    }

    return encounter;
  }

  @Transactional
  public PreHospitalEncounter arrive(
      UUID id) {

    PreHospitalEncounter encounter =
        get(id);

    encounter.markArrived();

    repo.save(encounter);

    outbox.append(
        "pre-hospital-encounter-arrived",
        id.toString(),
        new EncounterArrived(
            id,
            encounter.getPatientId(),
            encounter.getVisitId(),
            encounter.getAmbulanceId(),
            encounter.getDestinationFacilityId(),
            encounter.getArrivedAt()));

    return encounter;
  }

  @Transactional
  public PreHospitalEncounter cancel(
      UUID id,
      String reason) {

    PreHospitalEncounter encounter =
        get(id);

    encounter.cancel(reason);

    repo.save(encounter);

    outbox.append(
        "pre-hospital-encounter-cancelled",
        id.toString(),
        new EncounterCancelled(
            id,
            encounter.getPatientId(),
            encounter.getVisitId(),
            encounter.getAmbulanceId(),
            encounter.getDestinationFacilityId(),
            encounter.getCancellationReason(),
            encounter.getCancelledAt()));

    return encounter;
  }

  private Ambulance
      requireOperationalAmbulance(
          String ambulanceId) {

    if (ambulanceId == null ||
        ambulanceId.trim().isEmpty()) {

      throw new IllegalArgumentException(
          "Ambulance id is required");
    }

    String normalizedId =
        ambulanceId.trim();

    Ambulance ambulance =
        ambulanceRepository
            .findById(normalizedId)
            .orElseThrow(
                () ->
                    new PreHospitalNotFoundException(
                        "Ambulance not found: " +
                        normalizedId));

    if (!ambulance.isOperational()) {

      throw new PreHospitalConflictException(
          "Ambulance is not operational: " +
          normalizedId);
    }

    return ambulance;
  }

  private void requireActiveCoverage(
      String ambulanceId) {

    if (coverageRepository
        .findByAmbulanceIdAndEndedAtIsNull(
            ambulanceId)
        .isEmpty()) {

      throw new PreHospitalConflictException(
          "Ambulance has no active coverage: " +
          ambulanceId);
    }
  }

  public record EncounterCreated(
      UUID encounterId,
      UUID patientId,
      UUID visitId,
      String ambulanceId,
      UUID facilityId,
      Instant eta) {
  }

  public record PreArrivalAlert(
      UUID encounterId,
      UUID patientId,
      UUID visitId,
      UUID facilityId,
      String riskLevel,
      Instant eta) {
  }

  public record EncounterArrived(
      UUID encounterId,
      UUID patientId,
      UUID visitId,
      String ambulanceId,
      UUID facilityId,
      Instant arrivedAt) {
  }

  public record EncounterCancelled(
      UUID encounterId,
      UUID patientId,
      UUID visitId,
      String ambulanceId,
      UUID facilityId,
      String reason,
      Instant cancelledAt) {
  }
}
