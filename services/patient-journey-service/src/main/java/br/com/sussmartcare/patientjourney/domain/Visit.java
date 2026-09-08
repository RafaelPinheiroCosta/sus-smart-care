package br.com.sussmartcare.patientjourney.domain;
import java.time.*; import java.util.*;
public class Visit {
  private UUID id; private UUID patientId; private UUID facilityId; private Channel channel; private VisitStatus status; private String preAnamnesis; private Instant createdAt; private Instant checkedInAt; private long version;
  private final List<VisitDomainEvent> changes=new ArrayList<>();
  private Visit(){}
  public static Visit create(UUID patientId,UUID facilityId,Channel channel){var v=new Visit();v.raise(new VisitDomainEvent.PreVisitCreated(UUID.randomUUID(),patientId,facilityId,channel,Instant.now()));return v;}
  public static Visit rehydrate(List<VisitDomainEvent> events){var v=new Visit();events.forEach(v::apply);return v;}
  public void recordPreAnamnesis(String text){if(text==null||text.isBlank())throw new IllegalArgumentException("Pré-anamnese é obrigatória");raise(new VisitDomainEvent.PreAnamnesisRecorded(text,Instant.now()));}
  public void checkIn(){if(status!=VisitStatus.PRE_ARRIVAL)throw new IllegalStateException("Check-in permitido apenas em PRE_ARRIVAL");raise(new VisitDomainEvent.PatientCheckedIn(Instant.now()));}
  private void raise(VisitDomainEvent e){apply(e);changes.add(e);} private void apply(VisitDomainEvent e){
    if(e instanceof VisitDomainEvent.PreVisitCreated x){id=x.visitId();patientId=x.patientId();facilityId=x.facilityId();channel=x.channel();status=VisitStatus.PRE_ARRIVAL;createdAt=x.occurredAt();}
    else if(e instanceof VisitDomainEvent.PreAnamnesisRecorded x){preAnamnesis=x.text();}
    else if(e instanceof VisitDomainEvent.PatientCheckedIn x){status=VisitStatus.WAITING_TRIAGE;checkedInAt=x.occurredAt();}
    version++;
  }
  public List<VisitDomainEvent> pullChanges(){var copy=List.copyOf(changes);changes.clear();return copy;}
  public UUID getId(){return id;} public UUID getPatientId(){return patientId;} public UUID getFacilityId(){return facilityId;} public Channel getChannel(){return channel;} public VisitStatus getStatus(){return status;} public String getPreAnamnesis(){return preAnamnesis;} public Instant getCreatedAt(){return createdAt;} public Instant getCheckedInAt(){return checkedInAt;} public long getVersion(){return version;}
}
