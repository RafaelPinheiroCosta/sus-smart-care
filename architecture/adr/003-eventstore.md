# ADR-003 — Event Sourcing somente onde agrega valor

**Status:** Aceito; refinado pelo ADR-016

## Decisão
Event Sourcing é seletivo, principalmente em Patient Journey e Triage. Kafka não é tratado como Event Store.

A implementação do MVP persiste os streams append-only em PostgreSQL por simplicidade de execução local. O domínio permanece desacoplado do mecanismo de armazenamento para permitir um adapter especializado no futuro, caso a necessidade justifique.

## Consequências
Cadastros, fila, notificações e demais contextos continuam state-based quando a reconstrução histórica do agregado não oferece benefício proporcional à complexidade.
