# ADR-005 — Persistência por necessidade

**Status:** Aceito; refinado pelos ADR-016 e ADR-017

## Decisão
- PostgreSQL é a persistência transacional principal, inclusive para os streams Event Sourcing do MVP e para read models persistentes.
- Redis é usado onde dados quentes/temporais reduzem latência ou volume de escrita, como Clinical Query e rolling telemetry.
- EventStoreDB permanece uma possibilidade de adapter especializado, sem dependência no runtime atual.

## Consequências
A arquitetura evita introduzir uma tecnologia de persistência apenas por repertório; cada componente precisa justificar seu custo operacional.
