package br.com.sussmartcare.patientjourney.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Visit {

  private UUID id;
  private UUID patientId;
  private UUID facilityId;
  private Channel channel;
  private VisitStatus status;

  private String preAnamnesis;

  private String currentStage;

  private JourneyOutcome outcome;

  private UUID transferFacilityId;

  private String terminalNote;

  private Instant createdAt;
  private Instant checkedInAt;
  private Instant lastTransitionAt;
  private Instant completedAt;

  private long version;

  private final List<VisitDomainEvent> changes =
      new ArrayList<>();

  private Visit() {
  }

  public static Visit create(
      UUID patientId,
      UUID facilityId,
      Channel channel) {

    if (patientId == null) {
      throw new IllegalArgumentException(
          "Patient id is required");
    }

    if (facilityId == null) {
      throw new IllegalArgumentException(
          "Facility id is required");
    }

    if (channel == null) {
      throw new IllegalArgumentException(
          "Channel is required");
    }

    Visit visit =
        new Visit();

    visit.raise(
        new VisitDomainEvent.PreVisitCreated(
            UUID.randomUUID(),
            patientId,
            facilityId,
            channel,
            Instant.now()));

    return visit;
  }

  public static Visit rehydrate(
      List<VisitDomainEvent> events) {

    if (events == null ||
        events.isEmpty()) {

      throw new IllegalArgumentException(
          "Visit event stream is required");
    }

    Visit visit =
        new Visit();

    events.forEach(
        visit::apply);

    return visit;
  }

  public void recordPreAnamnesis(
      String text) {

    if (text == null ||
        text.isBlank()) {

      throw new IllegalArgumentException(
          "Pre-anamnesis is required");
    }

    ensureNotTerminal();

    raise(
        new VisitDomainEvent.PreAnamnesisRecorded(
            text.trim(),
            Instant.now()));
  }

  public void checkIn() {

    if (status != VisitStatus.PRE_ARRIVAL) {

      throw new IllegalStateException(
          "Check-in is allowed only from PRE_ARRIVAL");
    }

    raise(
        new VisitDomainEvent.PatientCheckedIn(
            Instant.now()));
  }

  public void startTriage() {

    requireStatus(
        VisitStatus.WAITING_TRIAGE);

    transition(
        VisitStatus.IN_TRIAGE,
        "TRIAGE",
        null,
        null,
        null);
  }

  public void completeTriage() {

    requireStatus(
        VisitStatus.IN_TRIAGE);

    transition(
        VisitStatus.TRIAGED,
        "TRIAGE",
        null,
        null,
        null);
  }

  public void queueForCare(
      String stage) {

    requireStatus(
        VisitStatus.TRIAGED);

    transition(
        VisitStatus.QUEUED,
        requireStage(stage),
        null,
        null,
        null);
  }

  public void call() {

    if (
        status != VisitStatus.QUEUED &&
        status != VisitStatus.WAITING_NEXT_STAGE
    ) {

      throw new IllegalStateException(
          "Patient can be called only from QUEUED or WAITING_NEXT_STAGE");
    }

    if (currentStage == null ||
        currentStage.isBlank()) {

      throw new IllegalStateException(
          "Current care stage is not defined");
    }

    transition(
        VisitStatus.CALLED,
        currentStage,
        null,
        null,
        null);
  }

  public void startService() {

    requireStatus(
        VisitStatus.CALLED);

    transition(
        VisitStatus.IN_SERVICE,
        currentStage,
        null,
        null,
        null);
  }

  public void waitForNextStage(
      String stage) {

    requireStatus(
        VisitStatus.IN_SERVICE);

    transition(
        VisitStatus.WAITING_NEXT_STAGE,
        requireStage(stage),
        null,
        null,
        null);
  }

  public void discharge(
      String note) {

    requireStatus(
        VisitStatus.IN_SERVICE);

    transition(
        VisitStatus.COMPLETED,
        currentStage,
        JourneyOutcome.DISCHARGED,
        null,
        normalizeNote(note));
  }

  public void transfer(
      UUID targetFacilityId,
      String note) {

    requireStatus(
        VisitStatus.IN_SERVICE);

    if (targetFacilityId == null) {

      throw new IllegalArgumentException(
          "Target facility id is required");
    }

    if (targetFacilityId.equals(facilityId)) {

      throw new IllegalArgumentException(
          "Transfer target must be another facility");
    }

    transition(
        VisitStatus.COMPLETED,
        currentStage,
        JourneyOutcome.TRANSFERRED,
        targetFacilityId,
        normalizeNote(note));
  }

  public void cancel(
      String reason) {

    ensureNotTerminal();

    if (reason == null ||
        reason.isBlank()) {

      throw new IllegalArgumentException(
          "Cancellation reason is required");
    }

    transition(
        VisitStatus.CANCELLED,
        currentStage,
        JourneyOutcome.CANCELLED,
        null,
        reason.trim());
  }

  private void transition(
      VisitStatus targetStatus,
      String stage,
      JourneyOutcome outcome,
      UUID transferFacilityId,
      String note) {

    raise(
        new VisitDomainEvent.JourneyTransitioned(
            targetStatus,
            stage,
            outcome,
            transferFacilityId,
            note,
            Instant.now()));
  }

  private void requireStatus(
      VisitStatus expected) {

    ensureNotTerminal();

    if (status != expected) {

      throw new IllegalStateException(
          "Expected visit status " +
          expected +
          " but current status is " +
          status);
    }
  }

  private void ensureNotTerminal() {

    if (
        status == VisitStatus.COMPLETED ||
        status == VisitStatus.CANCELLED
    ) {

      throw new IllegalStateException(
          "Visit is already terminal");
    }
  }

  private String requireStage(
      String stage) {

    if (stage == null ||
        stage.isBlank()) {

      throw new IllegalArgumentException(
          "Care stage is required");
    }

    return stage
        .trim()
        .toUpperCase();
  }

  private String normalizeNote(
      String note) {

    if (note == null ||
        note.isBlank()) {

      return null;
    }

    return note.trim();
  }

  private void raise(
      VisitDomainEvent event) {

    apply(event);
    changes.add(event);
  }

  private void apply(
      VisitDomainEvent event) {

    if (
        event instanceof
            VisitDomainEvent.PreVisitCreated created
    ) {

      id =
          created.visitId();

      patientId =
          created.patientId();

      facilityId =
          created.facilityId();

      channel =
          created.channel();

      status =
          VisitStatus.PRE_ARRIVAL;

      createdAt =
          created.occurredAt();

      lastTransitionAt =
          created.occurredAt();
    }
    else if (
        event instanceof
            VisitDomainEvent.PreAnamnesisRecorded anamnesis
    ) {

      preAnamnesis =
          anamnesis.text();
    }
    else if (
        event instanceof
            VisitDomainEvent.PatientCheckedIn checkedIn
    ) {

      status =
          VisitStatus.WAITING_TRIAGE;

      currentStage =
          "TRIAGE";

      checkedInAt =
          checkedIn.occurredAt();

      lastTransitionAt =
          checkedIn.occurredAt();
    }
    else if (
        event instanceof
            VisitDomainEvent.JourneyTransitioned transition
    ) {

      status =
          transition.status();

      currentStage =
          transition.currentStage();

      outcome =
          transition.outcome();

      transferFacilityId =
          transition.transferFacilityId();

      terminalNote =
          transition.note();

      lastTransitionAt =
          transition.occurredAt();

      if (
          status == VisitStatus.COMPLETED ||
          status == VisitStatus.CANCELLED
      ) {

        completedAt =
            transition.occurredAt();
      }
    }

    version++;
  }

  public List<VisitDomainEvent> pullChanges() {

    List<VisitDomainEvent> copy =
        List.copyOf(changes);

    changes.clear();

    return copy;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPatientId() {
    return patientId;
  }

  public UUID getFacilityId() {
    return facilityId;
  }

  public Channel getChannel() {
    return channel;
  }

  public VisitStatus getStatus() {
    return status;
  }

  public String getPreAnamnesis() {
    return preAnamnesis;
  }

  public String getCurrentStage() {
    return currentStage;
  }

  public JourneyOutcome getOutcome() {
    return outcome;
  }

  public UUID getTransferFacilityId() {
    return transferFacilityId;
  }

  public String getTerminalNote() {
    return terminalNote;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public Instant getLastTransitionAt() {
    return lastTransitionAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public long getVersion() {
    return version;
  }
}
