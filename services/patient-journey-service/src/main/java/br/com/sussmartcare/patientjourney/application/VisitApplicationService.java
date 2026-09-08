package br.com.sussmartcare.patientjourney.application;

import br.com.sussmartcare.patientjourney.domain.Channel;
import br.com.sussmartcare.patientjourney.domain.Visit;
import br.com.sussmartcare.patientjourney.domain.VisitRepository;
import br.com.sussmartcare.patientjourney.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitApplicationService {

  private final VisitRepository repo;
  private final EventOutbox outbox;
  private final PatientAccessPort patientAccess;

  public VisitApplicationService(
      VisitRepository repo,
      EventOutbox outbox,
      PatientAccessPort patientAccess) {

    this.repo = repo;
    this.outbox = outbox;
    this.patientAccess = patientAccess;
  }

  @Transactional
  public Visit create(
      UUID patientId,
      UUID facilityId,
      Channel channel) {

    patientAccess.assertCurrentActorCanAccess(patientId);

    Visit visit =
        Visit.create(patientId, facilityId, channel);

    repo.save(visit);

    outbox.append(
        "pre-visit-created",
        visit.getId().toString(),
        new PreVisitCreated(
            visit.getId(),
            patientId,
            facilityId,
            channel.name(),
            visit.getCreatedAt()));

    return visit;
  }

  public Visit get(UUID id) {

    Visit visit = load(id);

    patientAccess.assertCurrentActorCanAccess(
        visit.getPatientId());

    return visit;
  }

  @Transactional
  public Visit anamnesis(
      UUID id,
      String text) {

    Visit visit = load(id);

    patientAccess.assertCurrentActorCanAccess(
        visit.getPatientId());

    visit.recordPreAnamnesis(text);
    repo.save(visit);

    outbox.append(
        "pre-anamnesis-recorded",
        id.toString(),
        new PreAnamnesisRecorded(
            id,
            visit.getPatientId(),
            text,
            Instant.now()));

    return visit;
  }

  @Transactional
  public Visit checkIn(UUID id) {

    Visit visit = load(id);

    patientAccess.assertCurrentActorCanAccess(
        visit.getPatientId());

    visit.checkIn();
    repo.save(visit);

    outbox.append(
        "patient-checked-in",
        id.toString(),
        new PatientCheckedIn(
            id,
            visit.getPatientId(),
            visit.getFacilityId(),
            visit.getCheckedInAt()));

    return visit;
  }

  private Visit load(UUID id) {

    return repo.findById(id)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "Visita nao encontrada"));
  }

  public record PreVisitCreated(
      UUID visitId,
      UUID patientId,
      UUID facilityId,
      String channel,
      Instant createdAt) {}

  public record PreAnamnesisRecorded(
      UUID visitId,
      UUID patientId,
      String text,
      Instant occurredAt) {}

  public record PatientCheckedIn(
      UUID visitId,
      UUID patientId,
      UUID facilityId,
      Instant checkedInAt) {}
}