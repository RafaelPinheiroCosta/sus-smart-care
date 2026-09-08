# ADR-015 — Envelope de eventos, idempotência e DLT
**Decisão:** todos os eventos de integração publicados via Outbox usam envelope versionado (`eventId`, `eventType`, `aggregateId`, `occurredAt`, `schemaVersion`, `correlationId`, `traceId`, `data`). Consumidores críticos persistem `eventId` processado na mesma transação da projeção. Após retries limitados, mensagens problemáticas seguem para `<topic>.DLT`.
**Motivo:** entrega Kafka é no mínimo uma vez; duplicidade e poison messages não podem corromper projeções nem bloquear consumidores.
