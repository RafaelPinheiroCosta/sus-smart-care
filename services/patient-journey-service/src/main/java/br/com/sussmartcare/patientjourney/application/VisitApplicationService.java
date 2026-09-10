package br.com.sussmartcare.patientjourney.application;

import br.com.sussmartcare.patientjourney.domain.Channel;
import br.com.sussmartcare.patientjourney.domain.JourneyOutcome;
import br.com.sussmartcare.patientjourney.domain.Visit;
import br.com.sussmartcare.patientjourney.domain.VisitRepository;
import br.com.sussmartcare.patientjourney.domain.VisitStatus;
import br.com.sussmartcare.patientjourney.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
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

    patientAccess
        .assertCurrentActorCanAccess(
            patientId);

    Visit visit =
        Visit.create(
            patientId,
            facilityId,
            channel);

    repo.save(
        visit);

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

  public Visit get(
      UUID id) {

    Visit visit =
        load(id);

    patientAccess
        .assertCurrentActorCanAccess(
            visit.getPatientId());

    return visit;
  }

  @Transactional
  public Visit anamnesis(
      UUID id,
      String text) {

    Visit visit =
        authorized(id);

    visit.recordPreAnamnesis(
        text);

    repo.save(
        visit);

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
  public Visit checkIn(
      UUID id) {

    Visit visit =
        authorized(id);

    visit.checkIn();

    repo.save(
        visit);

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

  @Transactional
  public Visit startTriage(
      UUID id) {

    return transition(
        id,
        Visit::startTriage,
        "journey-triage-started");
  }

  @Transactional
  public Visit completeTriage(
      UUID id) {

    return transition(
        id,
        Visit::completeTriage,
        "journey-triage-completed");
  }

  @Transactional
  public Visit queue(
      UUID id,
      String stage) {

    return transition(
        id,
        visit ->
            visit.queueForCare(stage),
        "journey-queued");
  }

  @Transactional
  public Visit call(
      UUID id) {

    return transition(
        id,
        Visit::call,
        "journey-patient-called");
  }

  @Transactional
  public Visit startService(
      UUID id) {

    return transition(
        id,
        Visit::startService,
        "journey-service-started");
  }

  @Transactional
  public Visit nextStage(
      UUID id,
      String stage) {

    return transition(
        id,
        visit ->
            visit.waitForNextStage(stage),
        "journey-next-stage-required");
  }

  @Transactional
  public Visit discharge(
      UUID id,
      String note) {

    return transition(
        id,
        visit ->
            visit.discharge(note),
        "patient-discharged");
  }

  @Transactional
  public Visit transfer(
      UUID id,
      UUID targetFacilityId,
      String note) {

    return transition(
        id,
        visit ->
            visit.transfer(
                targetFacilityId,
                note),
        "patient-transferred");
  }

  @Transactional
  public Visit cancel(
      UUID id,
      String reason) {

    return transition(
        id,
        visit ->
            visit.cancel(reason),
        "journey-cancelled");
  }

  private Visit transition(
      UUID id,
      Consumer<Visit> mutation,
      String topic) {

    Visit visit =
        authorized(id);

    mutation.accept(
        visit);

    repo.save(
        visit);

    publishTransition(
        topic,
        visit);

    return visit;
  }

  private void publishTransition(
      String topic,
      Visit visit) {

    outbox.append(
        topic,
        visit.getId().toString(),
        new JourneyTransition(
            visit.getId(),
            visit.getPatientId(),
            visit.getFacilityId(),
            visit.getStatus(),
            visit.getCurrentStage(),
            visit.getOutcome(),
            visit.getTransferFacilityId(),
            visit.getTerminalNote(),
            visit.getLastTransitionAt()));
  }

  private Visit authorized(
      UUID id) {

    Visit visit =
        load(id);

    patientAccess
        .assertCurrentActorCanAccess(
            visit.getPatientId());

    return visit;
  }

  private Visit load(
      UUID id) {

    return repo
        .findById(id)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Visit not found"));
  }

  public record PreVisitCreated(
      UUID visitId,
      UUID patientId,
      UUID facilityId,
      String channel,
      Instant createdAt) {
  }

  public record PreAnamnesisRecorded(
      UUID visitId,
      UUID patientId,
      String text,
      Instant occurredAt) {
  }

  public record PatientCheckedIn(
      UUID visitId,
      UUID patientId,
      UUID facilityId,
      Instant checkedInAt) {
  }

  public record JourneyTransition(
      UUID visitId,
      UUID patientId,
      UUID facilityId,
      VisitStatus status,
      String currentStage,
      JourneyOutcome outcome,
      UUID transferFacilityId,
      String note,
      Instant occurredAt) {
  }
}
