package br.com.sussmartcare.patientjourney.infrastructure.eventsourcing;

import br.com.sussmartcare.patientjourney.domain.Visit;
import br.com.sussmartcare.patientjourney.domain.VisitDomainEvent;
import br.com.sussmartcare.patientjourney.domain.VisitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EventSourcedVisitRepository
    implements VisitRepository {

  private final VisitStoredEventRepository events;
  private final ObjectMapper mapper;

  public EventSourcedVisitRepository(
      VisitStoredEventRepository events,
      ObjectMapper mapper) {

    this.events = events;
    this.mapper = mapper;
  }

  @Override
  public Visit save(
      Visit visit) {

    var changes =
        visit.pullChanges();

    long version =
        visit.getVersion() -
        changes.size() +
        1;

    for (var change : changes) {

      events.save(
          new VisitStoredEvent(
              visit.getId(),
              version++,
              type(change),
              json(change),
              change.occurredAt()));
    }

    return visit;
  }

  @Override
  public Optional<Visit> findById(
      UUID id) {

    var stored =
        events.findByStreamIdOrderByEventVersionAsc(
            id);

    if (stored.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        Visit.rehydrate(
            stored
                .stream()
                .map(this::decode)
                .toList()));
  }

  private String type(
      VisitDomainEvent event) {

    return event
        .getClass()
        .getSimpleName();
  }

  private String json(
      Object value) {

    try {

      return mapper.writeValueAsString(
          value);
    }
    catch (Exception exception) {

      throw new IllegalStateException(
          "Could not serialize visit event",
          exception);
    }
  }

  private VisitDomainEvent decode(
      VisitStoredEvent stored) {

    try {

      return switch (
          stored.getEventType()
      ) {

        case "PreVisitCreated" ->
            mapper.readValue(
                stored.getPayload(),
                VisitDomainEvent
                    .PreVisitCreated.class);

        case "PreAnamnesisRecorded" ->
            mapper.readValue(
                stored.getPayload(),
                VisitDomainEvent
                    .PreAnamnesisRecorded.class);

        case "PatientCheckedIn" ->
            mapper.readValue(
                stored.getPayload(),
                VisitDomainEvent
                    .PatientCheckedIn.class);

        case "JourneyTransitioned" ->
            mapper.readValue(
                stored.getPayload(),
                VisitDomainEvent
                    .JourneyTransitioned.class);

        default ->
            throw new IllegalStateException(
                "Unknown patient journey event: " +
                stored.getEventType());
      };
    }
    catch (Exception exception) {

      throw new IllegalStateException(
          "Could not rehydrate visit",
          exception);
    }
  }
}
