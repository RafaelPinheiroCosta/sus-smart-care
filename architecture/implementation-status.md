# Implementation Status — v0.4

## Implementado na baseline
- 9 bounded contexts/deployables + API Gateway + Clinical Query read side.
- Patient Registry canônico com busca por identificador e histórico independente de conta/representante.
- Transactional Outbox, envelope versionado, correlação e consumidores idempotentes/DLT nos pontos críticos.
- Choreography real: check-in → triagem; prioridade confirmada → fila; eventos → View Data.
- Event Sourcing seletivo em `Visit` e `Triage`.
- Telemetry separado, com dispositivo/sessão/normalização e resumo estatístico não clínico.
- IA atrás de `RiskAssessmentPort` + Resilience4j + fallback humano.
- Queue com prioridade clínica separada de presença/readiness e experiência pública anonimizada.
- Notification por adapters/canais e paciente sem smartphone.
- Keycloak com papéis de paciente, representante, profissionais, ambulância e device client.
- Autorização coarse-grained por papel em cada serviço.
- OpenAPI completo dos boundaries HTTP e AsyncAPI tipado para eventos centrais.
- Docker Compose de infraestrutura e `docker-compose.full.yml` com todos os deployables.
- E2E PowerShell/Bash baseado em polling de consistência eventual.
- Testcontainers de persistência no Patient Registry, ArchUnit, JUnit, k6 e CI.
- OpenTelemetry/Micrometer, Prometheus, Tempo e Grafana.

## Ainda não considerado validado
- `mvn verify` completo: o runtime usado para gerar esta versão não dispõe de Maven/dependências externas.
- `docker compose -f docker-compose.full.yml up`: Docker não está disponível neste runtime.
- Fine-grained authorization/ownership por `sub` + vínculo de representação.
- Modelo de IA clinicamente validado.
- Integrações reais SAMU/HL7/FHIR/MQTT, push/SMS e hardware.
- EventStoreDB como adapter de Event Sourcing (PostgreSQL permanece adapter funcional no MVP).

## Critério para promover para v0.5
A primeira execução externa deve priorizar: `mvn verify`, Docker full-stack, E2E completo e correção de qualquer incompatibilidade de dependência/runtime encontrada. Só depois disso entra nova complexidade funcional.
