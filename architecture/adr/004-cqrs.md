# ADR — CQRS seletivo

**Status:** Aceito para baseline v0.1

## Contexto
O Tech Challenge exige um MVP demonstrável e o objetivo do projeto é consolidar os padrões aprendidos sem adicionar complexidade sem justificativa.

## Decisão
Aplicar CQRS principalmente em Queue e views clínicas/operacionais; serviços simples permanecem com modelo convencional.

## Consequências
- Benefícios devem ser demonstráveis no domínio e/ou operação.
- A decisão pode ser revisada após métricas, testes de carga ou evolução do Event Storming.
- Toda mudança breaking gera novo ADR ou revisão explícita.
