# ADR-016 — Event Sourcing seletivo em Journey e Triage
**Decisão:** `Patient Journey` e `Triage` reconstroem seus agregados a partir de streams append-only em `visit_events` e `triage_events`. O EventStore é abstraído pelo repositório; a baseline usa PostgreSQL para permitir execução local simples, mantendo EventStoreDB como alvo de evolução do adapter.
**Não aplicado:** cadastro, notificações e fila continuam state-based. Event Sourcing só é usado onde a história da decisão e da jornada possui valor de auditoria/replay.
