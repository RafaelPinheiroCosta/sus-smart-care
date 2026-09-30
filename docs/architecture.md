# Arquitetura do SUS Smart Care

Este documento descreve a arquitetura **atual** do MVP. Decisões históricas e justificativas específicas permanecem nos ADRs; contratos de endpoints e eventos permanecem em OpenAPI/AsyncAPI.

## 1. Origem da modelagem

A arquitetura começou pelo domínio, não pela escolha dos frameworks. O Event Storming foi utilizado para percorrer a jornada do paciente, identificar fatos relevantes que acontecem no processo, os comandos que os provocam, regras, exceções e pontos de decisão. A partir desse mapa foram definidos agregados e bounded contexts com responsabilidades distintas.

O resultado dessa separação é a base dos microsserviços atuais. O registro visual do workshop está em `architecture/event-storming/`.

## 2. Visão dos contextos e aplicações

| Contexto/aplicação | Responsabilidade |
|---|---|
| Facility | estrutura da unidade: `HealthFacility`, `CareZone`, `Bed` e `BedOccupation` |
| Patient Registry | identidade clínica canônica, identificadores, identidade provisória, merge e representação |
| Patient Journey | pré-visita, check-in e lifecycle macro da visita |
| Triage | observações de triagem, recomendação de risco e confirmação da prioridade clínica |
| Queue | ordenação operacional, posição estimada, chamada, retorno e missed-call policy |
| Presence | tracking temporal, sinais de presença, zonas e ocupação operacional |
| Telemetry | inventário de dispositivos, placement, assignment, sessões e biometria |
| Pre-Hospital | frota, cobertura, encounter pré-hospitalar, ETA, risco e chegada |
| Notification | seleção de canal e histórico de entrega/comunicação |
| Identity & Access | autenticação e identidade do ator, com Keycloak como IdP |
| Clinical Query | read side técnico de CQRS; não é novo domínio de negócio |
| API Gateway | edge da plataforma e roteamento dos boundaries HTTP |

## 3. Fluxo clínico e operacional

O paciente pode entrar pelo próprio canal digital, por responsável, recepção/totem ou ambulância. Todos os caminhos convergem para um `Patient` canônico. A jornada clínica permanece independente da existência de conta ou smartphone.

Patient Journey controla o lifecycle macro da visita. Triage é a autoridade da prioridade clínica. Queue é a autoridade da ordenação e readiness operacional. Presence informa onde o paciente está ou se saiu temporariamente, mas nunca altera a prioridade clínica. Facility fornece a topologia assistencial usada para organizar unidades, zonas e leitos.

No fluxo de urgência, Pre-Hospital registra ambulância, cobertura, encounter, ETA e sinais de risco. Telemetry pode receber dados antes da chegada; esses dados antecipam preparação, mas a classificação clínica final continua sob responsabilidade profissional.

## 4. Comunicação síncrona e assíncrona

### REST

REST é usado quando o chamador precisa de resposta síncrona: administração, consultas, comandos humanos e validações pontuais entre contextos. O API Gateway expõe os boundaries em `/api/v1/**`.

### Kafka

Kafka é o backbone dos eventos de integração. Os eventos relevantes saem por Transactional Outbox e usam envelope versionado com `eventId`, `eventType`, `aggregateId`, `occurredAt`, `schemaVersion`, `correlationId`, `traceId` e `data`.

Consumidores críticos são idempotentes. Fluxos entre tópicos diferentes não assumem ordenação global. Retries são limitados e mensagens que continuam falhando podem seguir para DLT.

O contrato autoritativo está em `contracts/asyncapi/platform-events.yaml`.

### MQTT

MQTT é usado no boundary device-to-platform da telemetria. O Telemetry Service conecta-se ao broker Mosquitto no ambiente local e recebe mensagens no tópico:

```text
sus/v1/devices/{deviceExternalId}/telemetry
```

O payload do dispositivo carrega identidade da mensagem, sequência, tipo de medida, valor, unidade e instante da medição. Ele não carrega `patientId`, `visitId`, CPF ou CNS. O backend resolve a sessão clínica pelo `DeviceAssignment` ativo.

QoS 1 é combinado com deduplicação por `messageId` e `sequence`, evitando que redelivery gere duplicidade lógica.

## 5. Telemetria contínua

O Telemetry Service possui dois modos:

- `SPOT`: medições isoladas são persistidas como `BiometricObservation` e publicadas individualmente;
- `CONTINUOUS`: amostras normais entram em rolling window no Redis e alimentam agregados persistentes, reduzindo escrita/eventos por amostra.

Amostras anômalas são persistidas e publicadas imediatamente como sinal operacional. Esse mecanismo não substitui a avaliação de Triage.

## 6. Event Sourcing seletivo

Patient Journey e Triage preservam a sequência de mudanças relevantes em streams append-only (`visit_events` e `triage_events`) e reconstroem os agregados a partir desses eventos.

A implementação atual usa PostgreSQL por simplicidade operacional do MVP. EventStoreDB permanece desacoplado da regra de domínio e pode ser introduzido por adapter em evolução futura; o runtime atual não depende dele.

## 7. CQRS e leitura clínica

Clinical Query consome eventos dos contextos e mantém `ClinicalPatientView` como projeção de leitura. PostgreSQL é a cópia persistente e Redis funciona como cache quente com fallback para o read model persistente.

A projeção é eventualmente consistente. Por isso a decisão clínica não depende exclusivamente dela e os testes E2E usam polling quando aguardam convergência.

## 8. IA com human-in-the-loop

Triage depende de `RiskAssessmentPort`. O provider HTTP é protegido com Resilience4j; indisponibilidade do provider produz fallback e não impede a triagem. O provider `demo` existe apenas para demonstrar a integração arquitetural.

A recomendação da IA é uma informação de apoio. A prioridade final é confirmada por profissional autenticado e a identidade desse profissional é derivada do JWT, não de um campo enviado pelo cliente.

## 9. Presença e inclusão

Presence é independente da tecnologia de captura. O domínio suporta sinais `BLE`, `WIFI`, `UWB`, `QR`, `KIOSK`, `MANUAL` e `SYSTEM`. Sessões de tracking usam token opaco e apenas o hash é persistido.

Sinais físicos apontam para gateways registrados; sinais manuais e de sistema permitem atender pacientes sem smartphone. A descoberta passiva/automática de celulares por Wi-Fi/Bluetooth é evolução futura e não deve ser confundida com funcionalidade já implantada.

## 10. Diagramas

- `architecture/diagrams/system-context.mmd`
- `architecture/diagrams/container-view.mmd`
- `architecture/diagrams/sequence-ambulance.mmd`
- `architecture/diagrams/sequence-no-smartphone.mmd`

Os ADRs em `architecture/adr/` registram as decisões e trade-offs que levaram a esta arquitetura.
