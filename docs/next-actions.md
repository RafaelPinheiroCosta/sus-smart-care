# Proximas acoes - v0.4.1

A etapa de estabilizacao da v0.4.1 foi encerrada.

## Baseline atual

A v0.4.1 esta validada como baseline estavel.

Foram concluidas as etapas:

- 0.1 CI;
- 0.2 E2E representation;
- 0.3 Queue authorization;
- 0.4 Presence authorization;
- 0.5 Notification authorization;
- 0.6 Triage professional identity;
- 0.7 communication profile ownership;
- 0.8 contracts and HTTP semantics;
- 0.9 documentation;
- 0.10 full regression.

A regressao final confirmou:

- Maven reactor completo verde;
- stack completa operacional;
- 11 health checks HTTP 200;
- containers sem restart inesperado;
- E2E principal verde;
- E2E de representacao verde;
- E2E sem smartphone verde.

O proximo trabalho funcional passa a ser o Stage 1.
## Proxima fase funcional

Depois da v0.4.1 estavel, iniciar o Stage 1.

### Stage 1 - Facility Service

Criar o bounded context responsavel pela estrutura assistencial da unidade.

Entidades inicialmente planejadas:

- HealthFacility
- CareZone
- Bed
- BedOccupation

Esse servico passara a fornecer identidade consistente das unidades e setores atualmente representados apenas por UUIDs nos demais contextos.

## Evolucoes seguintes planejadas

Apos Facility:

1. modelagem estruturada de Ambulance e Coverage;
2. evolucao de MedicalDevice;
3. DevicePlacement;
4. DeviceAssignment;
5. TelemetrySession evoluida;
6. MQTT device-to-platform;
7. deduplicacao por messageId e sequence;
8. Redis para rolling telemetry;
9. agregacoes e anomalias;
10. Presence avancado;
11. extensao da jornada emergencial;
12. Clinical Query consolidada para demonstracao final.

## Principios preservados

- nao criar complexidade antes de fechar a regressao anterior;
- Patient permanece separado de UserAccount;
- prioridade clinica continua humana e independente do canal;
- MQTT sera usado no boundary de dispositivos;
- Kafka continuara como backbone de eventos internos;
- REST sera utilizado para administracao, consulta e acoes humanas;
- Notification continuara sendo projecao do perfil de comunicacao do Patient Registry;
- nenhuma telemetria de dispositivo dependera de CPF, CNS ou outro identificador clinico no payload do dispositivo.
