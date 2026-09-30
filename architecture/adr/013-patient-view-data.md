# ADR-013 — View Data clínica

**Status:** Aceito e parcialmente implementado

## Decisão
Perfis de leitura não fazem fan-out síncrono para vários microsserviços a cada tela. O `Clinical Query Service` mantém `ClinicalPatientView` como projeção CQRS persistente em PostgreSQL e cacheada em Redis.

Outras views orientadas à tarefa podem ser extraídas quando houver necessidade real de produto/carga. A public view da fila permanece anonimizada.

## Motivo
Separar leitura de escrita reduz acoplamento e permite otimizar consultas sem transformar um serviço de domínio em agregador genérico.
