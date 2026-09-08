# ADR — EventStoreDB apenas onde Event Sourcing agrega valor

**Status:** Aceito para baseline v0.1

## Contexto
O Tech Challenge exige um MVP demonstrável e o objetivo do projeto é consolidar os padrões aprendidos sem adicionar complexidade sem justificativa.

## Decisão
Event Sourcing será seletivo (principalmente Journey/Triage). Kafka não será tratado como Event Store.

## Consequências
- Benefícios devem ser demonstráveis no domínio e/ou operação.
- A decisão pode ser revisada após métricas, testes de carga ou evolução do Event Storming.
- Toda mudança breaking gera novo ADR ou revisão explícita.
