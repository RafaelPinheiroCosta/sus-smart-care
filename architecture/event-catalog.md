# Catálogo de eventos de integração — v0.4

Todos os tópicos usam envelope versionado (`eventId`, `eventType`, `aggregateId`, `occurredAt`, `schemaVersion`, `correlationId`, `traceId`, `data`). `eventId` é a chave de idempotência do consumer.

| Tópico | Produtor | Consumidores principais | Finalidade |
|---|---|---|---|
| `patient-registered` | Patient Registry | Clinical Query | criar perfil canônico |
| `patient-provisional-created` | Patient Registry | Clinical Query | identidade provisória rastreável |
| `patient-identity-resolved` | Patient Registry | Clinical Query | confirmar identidade sem trocar `patientId` |
| `patient-profile-updated` | Patient Registry | Clinical Query | atualizar projeção demográfica |
| `patient-records-merged` | Patient Registry | Clinical Query, Notification | apontar histórico ao paciente canônico |
| `patient-representative-linked` | Patient Registry | auditoria futuro | vínculo temporal de representação |
| `patient-representative-revoked` | Patient Registry | auditoria futuro | revogação de vínculo |
| `pre-visit-created` | Journey | Queue, Clinical Query, Notification | mapear paciente/facility/visita |
| `pre-anamnesis-recorded` | Journey | Clinical Query | enriquecer View Data |
| `patient-checked-in` | Journey | Triage, Clinical Query | iniciar triagem idempotentemente |
| `triage-started` | Triage | analytics futuro | abertura da triagem |
| `biometric-observation-received` | Telemetry | Triage, Clinical Query | observação biométrica normalizada |
| `triage-assessment-generated` | Triage | Clinical Query | recomendação, confiança e versão do provider |
| `clinical-priority-confirmed` | Triage | Queue, Clinical Query | decisão humana final |
| `patient-presence-changed` | Presence | Queue, Clinical Query | presença/localização lógica |
| `patient-queued` | Queue | analytics futuro | entrada operacional na fila |
| `queue-position-estimated` | Queue | Clinical Query | posição/tempo projetados |
| `queue-return-required` | Queue | Notification | solicitar retorno à unidade |
| `queue-entry-state-changed` | Queue | analytics futuro | mudança operacional/readiness |
| `patient-returned-to-queue` | Queue | analytics futuro | retorno efetivo |
| `patient-called` | Queue | Notification | chamada adaptativa |
| `pre-hospital-encounter-created` | Pre-Hospital | analytics futuro | atendimento antes da chegada |
| `pre-arrival-alert-created` | Pre-Hospital | Notification, Clinical Query | preparação antecipada da unidade |

O contrato detalhado está em `contracts/asyncapi/platform-events.yaml`.
