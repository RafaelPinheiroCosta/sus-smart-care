package br.com.sussmartcare.patientjourney.domain;

import java.time.Instant;
import java.util.UUID;

public sealed interface VisitDomainEvent
    permits
        VisitDomainEvent.PreVisitCreated,
        VisitDomainEvent.PreAnamnesisRecorded,
        VisitDomainEvent.PatientCheckedIn,
        VisitDomainEvent.JourneyTransitioned {

  Instant occurredAt();

  record PreVisitCreated(
      UUID visitId,
      UUID patientId,
      UUID facilityId,
      Channel channel,
      Instant occurredAt)
      implements VisitDomainEvent {
  }

  record PreAnamnesisRecorded(
      String text,
      Instant occurredAt)
      implements VisitDomainEvent {
  }

  record PatientCheckedIn(
      Instant occurredAt)
      implements VisitDomainEvent {
  }

  record JourneyTransitioned(
      VisitStatus status,
      String currentStage,
      JourneyOutcome outcome,
      UUID transferFacilityId,
      String note,
      Instant occurredAt)
      implements VisitDomainEvent {
  }
}
