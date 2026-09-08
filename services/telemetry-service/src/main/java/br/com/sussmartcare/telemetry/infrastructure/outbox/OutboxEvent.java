package br.com.sussmartcare.telemetry.infrastructure.outbox;
import jakarta.persistence.*; import java.time.*; import java.util.*;
@Entity @Table(name="outbox_events") public class OutboxEvent {
  @Id private UUID id; @Column(nullable=false) private String topic; @Column(nullable=false) private String eventKey;
  @Column(nullable=false,length=16000) private String payload; @Column(nullable=false) private Instant createdAt; private Instant publishedAt;
  @Column(nullable=false) private int attempts; @Column(nullable=false) private Instant nextAttemptAt; @Column(length=2000) private String lastError;
  protected OutboxEvent(){}
  public OutboxEvent(String topic,String key,String payload){id=UUID.randomUUID();this.topic=topic;eventKey=key;this.payload=payload;createdAt=Instant.now();nextAttemptAt=createdAt;}
  public void published(){publishedAt=Instant.now();lastError=null;}
  public void failed(Exception error){attempts++;long seconds=Math.min(60L,(long)Math.pow(2,Math.min(attempts,6)));nextAttemptAt=Instant.now().plusSeconds(seconds);lastError=error.getClass().getSimpleName()+": "+String.valueOf(error.getMessage());if(lastError.length()>2000)lastError=lastError.substring(0,2000);}
  public UUID getId(){return id;} public String getTopic(){return topic;} public String getEventKey(){return eventKey;} public String getPayload(){return payload;}
  public Instant getPublishedAt(){return publishedAt;} public int getAttempts(){return attempts;} public Instant getNextAttemptAt(){return nextAttemptAt;} public String getLastError(){return lastError;}
}
