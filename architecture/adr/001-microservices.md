# ADR-001 — Microsserviços por capacidade de negócio

**Status:** Aceito e evoluído

## Contexto
A modelagem por Event Storming indicou capacidades com responsabilidades, ritmos de mudança e perfis de carga distintos. O monorepo facilita o trabalho de uma pessoa sem obrigar a aplicação a ser um monólito.

## Decisão
Separar serviços por capacidade de negócio, e não por entidade técnica. A arquitetura atual possui 10 contextos de negócio: Facility, Identity & Access, Patient Registry, Patient Journey, Triage, Telemetry, Queue, Presence, Pre-Hospital e Notification. Clinical Query é o read side técnico de CQRS e o API Gateway é o edge da plataforma.

## Consequências
- cada serviço mantém responsabilidade e persistência próprias;
- integrações síncronas são usadas somente quando a resposta imediata é necessária;
- eventos reduzem acoplamento nos fluxos de propagação;
- novos serviços só devem ser criados quando houver uma capacidade ou motivo de mudança realmente distinto.
