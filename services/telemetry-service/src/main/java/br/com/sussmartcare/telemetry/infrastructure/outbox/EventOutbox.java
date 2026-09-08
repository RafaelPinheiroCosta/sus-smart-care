package br.com.sussmartcare.telemetry.infrastructure.outbox;
import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.databind.ObjectMapper; import java.time.*; import java.util.*; import org.slf4j.MDC; import org.springframework.stereotype.Component;
@Component public class EventOutbox {
  private final OutboxRepository repo; private final ObjectMapper mapper; public EventOutbox(OutboxRepository r,ObjectMapper m){repo=r;mapper=m;}
  public void append(String topic,String key,Object event){
    try{var envelope=new IntegrationEventEnvelope(UUID.randomUUID(),topic,key,Instant.now(),"1",MDC.get("correlationId"),MDC.get("traceId"),event);repo.save(new OutboxEvent(topic,key,mapper.writeValueAsString(envelope)));}
    catch(JsonProcessingException e){throw new IllegalStateException("Falha serializando evento",e);}
  }
  public record IntegrationEventEnvelope(UUID eventId,String eventType,String aggregateId,Instant occurredAt,String schemaVersion,String correlationId,String traceId,Object data){}
}
