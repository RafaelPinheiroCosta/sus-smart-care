package br.com.sussmartcare.triage.domain; import java.time.*; import java.util.*;
public sealed interface TriageDomainEvent permits TriageDomainEvent.TriageStarted,TriageDomainEvent.TriageAssessed,TriageDomainEvent.ClinicalPriorityConfirmed {Instant occurredAt();
 record TriageStarted(UUID triageId,UUID visitId,Instant occurredAt) implements TriageDomainEvent{}
 record TriageAssessed(ClinicalPriority recommendation,double confidence,String reasoning,String modelName,String modelVersion,Instant occurredAt) implements TriageDomainEvent{}
 record ClinicalPriorityConfirmed(ClinicalPriority priority,UUID professionalId,Instant occurredAt) implements TriageDomainEvent{}
}
