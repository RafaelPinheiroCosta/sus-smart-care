# Context Map v0.1

## 1. Identity & Access
**Responsabilidade:** autenticação, autorização, clients, scopes e service accounts.  
**Não é dono:** Patient, histórico clínico, vínculo familiar.  
**Tecnologia alvo:** Keycloak no MVP; Spring Security Resource Server nos serviços.

## 2. Patient Registry
**Responsabilidade:** identidade canônica do paciente, identificadores, cadastro, identidade provisória, resolução/merge e vínculos de representação.  
**Agregados:** Patient, RepresentativeRelationship.  
**Invariantes:** histórico pertence ao Patient; um Patient pode existir sem UserAccount.

## 3. Patient Journey
**Responsabilidade:** PreVisit, Visit, CheckIn, canais de entrada e etapas da jornada.  
**Agregados:** PreVisit, Visit.  
**Publica:** PreVisitCreated, AnamnesisCompleted, PatientCheckedIn, JourneyStepChanged, VisitCompleted.

## 4. Triage
**Responsabilidade:** triagem, observações clínicas normalizadas, rule assessment, recomendação IA, decisão profissional e prioridade clínica.  
**Agregado:** Triage.  
**Regra:** IA nunca é autoridade final.

## 5. Telemetry & Device Integration
**Responsabilidade:** identidade técnica/capability de devices, adapters/protocolos, sessões de telemetria, ingestão, normalização, agregação e publicação de observações.  
**Agregados:** Device, TelemetrySession.  
**Característica:** alta taxa de escrita; escalabilidade independente.

## 6. Queue Management
**Responsabilidade:** fila clínica operacional, QueueEntry, score, posição estimada, espera, readiness, grace period e missed-call policy.  
**Agregados:** Queue, QueueEntry.  
**CQRS:** forte candidato; views de paciente, telão e operação.

## 7. Presence
**Responsabilidade:** presença lógica e transição entre zonas, independente da tecnologia de captura.  
**Fontes:** app/geofence, totem, recepção, QR, NFC, beacon, pulseira, confirmação de staff.

## 8. Pre-Hospital
**Responsabilidade:** PreHospitalEncounter, transporte, ambulância, ETA, destino e PreArrivalAlert.  
**Não é dono:** cadastro do paciente nem telemetria bruta.

## 9. Notification
**Responsabilidade:** roteamento de comunicação por canal, preferências, retries, status e adaptação de provedores.  
**Canais:** push, SMS, e-mail, telão, chamada verbal/staff, responsável.

## Relações principais
- Patient Journey -> Patient Registry (síncrona quando precisa validar/associar identidade).
- Triage consome eventos de Telemetry e consulta contexto mínimo da Visit.
- Queue consome ClinicalPriorityConfirmed e eventos de Presence.
- Notification consome eventos de Queue/Pre-Hospital/Journey.
- Pre-Hospital usa Patient Registry para identidade e Telemetry para sessão de dispositivos.
- Query models são alimentados por eventos e não viram um “mega serviço genérico” nesta fase.
