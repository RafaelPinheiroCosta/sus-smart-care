# Estratégia de testes

## 1. Domínio
JUnit testa invariantes sem Spring: máquina de estados de Visit/Triage, política de retorno da fila, merge/representação e normalização relevante.

## 2. Arquitetura
ArchUnit impede dependências proibidas entre domínio, aplicação e adapters. O objetivo é preservar Clean/Hexagonal conforme os serviços evoluem.

## 3. Persistência e integração
Testcontainers é a estratégia definida para PostgreSQL/Kafka/Redis. Os cenários prioritários são:
- migrations Flyway partindo de banco vazio;
- unicidade/concorrência de streams Event Sourcing;
- gravação transacional estado + Outbox;
- idempotência de consumers;
- reprocessamento de eventos fora de ordem;
- fallback do read side quando Redis falhar.

## 4. Contratos
OpenAPI e AsyncAPI passam por validação estrutural no CI. Evolução futura: consumer-driven contract tests para integrações síncronas que realmente existirem.

## 5. End-to-end
Cenários oficiais:
1. paciente digital;
2. paciente sem smartphone;
3. dependente/tutor;
4. paciente provisório e posterior merge;
5. ambulância + telemetria pré-hospitalar;
6. IA indisponível com decisão humana preservada.

## 6. Carga
k6 mede endpoints de leitura de fila/View Data. A taxa de telemetria deve ganhar cenário próprio quando o pipeline Kafka estiver sendo executado em ambiente integrado.

## 7. Segurança
Pipeline inclui dependency check. Evoluções: SAST, secret scanning, DAST sobre ambiente efêmero e testes de autorização por role/scope.

## Critério de saída para release acadêmica

- contratos válidos;
- migrations válidas;
- `mvn verify` verde em CI;
- testes de domínio verdes;
- fluxo E2E demonstrável;
- nenhum segredo real versionado;
- documentação/ADRs coerentes com a implementação;
- alertas de segurança críticos avaliados antes da entrega.
