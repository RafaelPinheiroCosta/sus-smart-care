# Proximas acoes - v0.4.1

A etapa de estabilizacao esta proxima do encerramento.

As etapas 0.1 a 0.8 foram concluidas.

## Etapa atual

### 0.9 - documentacao

Atualizar documentacao para refletir o estado real da implementacao e remover afirmacoes herdadas da baseline v0.4.

### 0.10 - regressao completa

Executar:

1. mvn clean verify;
2. validar docker compose -f docker-compose.full.yml;
3. confirmar health de todos os deployables;
4. executar o E2E principal;
5. executar o cenario de representacao;
6. executar o cenario de paciente sem smartphone;
7. verificar containers sem restart inesperado;
8. registrar a evidencia final em docs/validation-v0.4.1.md.

A v0.4.1 somente sera considerada encerrada depois dessa regressao.

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
