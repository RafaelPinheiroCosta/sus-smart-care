package br.com.sussmartcare.patientjourney.infrastructure.eventsourcing;
import br.com.sussmartcare.patientjourney.domain.*; import com.fasterxml.jackson.databind.ObjectMapper; import java.util.*; import org.springframework.stereotype.Repository;
@Repository public class EventSourcedVisitRepository implements VisitRepository {
  private final VisitStoredEventRepository events; private final ObjectMapper mapper; public EventSourcedVisitRepository(VisitStoredEventRepository e,ObjectMapper m){events=e;mapper=m;}
  public Visit save(Visit visit){var changes=visit.pullChanges();long version=visit.getVersion()-changes.size()+1;for(var change:changes){events.save(new VisitStoredEvent(visit.getId(),version++,type(change),json(change),change.occurredAt()));}return visit;}
  public Optional<Visit> findById(UUID id){var stored=events.findByStreamIdOrderByEventVersionAsc(id);if(stored.isEmpty())return Optional.empty();return Optional.of(Visit.rehydrate(stored.stream().map(this::decode).toList()));}
  private String type(VisitDomainEvent e){return e.getClass().getSimpleName();} private String json(Object o){try{return mapper.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
  private VisitDomainEvent decode(VisitStoredEvent e){try{return switch(e.getEventType()){case "PreVisitCreated"->mapper.readValue(e.getPayload(),VisitDomainEvent.PreVisitCreated.class);case "PreAnamnesisRecorded"->mapper.readValue(e.getPayload(),VisitDomainEvent.PreAnamnesisRecorded.class);case "PatientCheckedIn"->mapper.readValue(e.getPayload(),VisitDomainEvent.PatientCheckedIn.class);default->throw new IllegalStateException("Evento de jornada desconhecido: "+e.getEventType());};}catch(Exception ex){throw new IllegalStateException("Falha reidratando visita",ex);}}
}
