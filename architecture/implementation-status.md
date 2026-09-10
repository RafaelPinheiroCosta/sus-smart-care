# Implementation Status - v0.4.1

## Estado atual

A v0.4.1 representa a baseline de estabilizacao da plataforma antes da introducao de novos dominios e integracoes IoT avancadas.

As etapas 0.1 a 0.8 ja foram implementadas e validadas incrementalmente.

## Implementado e validado incrementalmente

- 9 bounded contexts de negocio.
- API Gateway.
- Clinical Query read side.
- Patient Registry canonico independente de conta de usuario.
- Identificadores clinicos e administrativos.
- Vinculos temporais e revogaveis de representacao.
- Vinculo SELF entre paciente e conta.
- Transactional Outbox.
- Eventos Kafka com envelope versionado.
- Consumidores idempotentes nos fluxos relevantes.
- Check-in orientado a eventos.
- Triage human-in-the-loop.
- IA encapsulada atras de RiskAssessmentPort.
- Queue separando prioridade clinica de estado operacional.
- Presence com controle de acesso por visita.
- Notification com controle de acesso por paciente.
- Telemetry com dispositivos, sessoes e observacoes normalizadas.
- Pre-Hospital com fluxo de pre-chegada.
- Keycloak como provedor de identidade.
- Roles para paciente, representante, profissionais, operador, ambulancia e device.
- OpenAPI dos boundaries HTTP.
- AsyncAPI dos principais eventos.
- Docker Compose da stack.
- JUnit, ArchUnit e Testcontainers.
- Observabilidade com Micrometer, OpenTelemetry, Prometheus, Tempo e Grafana.

## Seguranca da v0.4.1

A plataforma nao depende mais apenas de autorizacao coarse-grained por role.

Foi introduzida autorizacao fine-grained nos principais recursos com risco de BOLA/IDOR.

### Patient Registry

- Patient pertence ao dominio e nao a conta.
- acesso avaliado por PatientAccessApplicationService;
- paciente pode acessar seu proprio registro via vinculo SELF;
- representante precisa possuir representacao ativa;
- profissionais e perfis operacionais autorizados podem acessar conforme role;
- perfil de comunicacao aplica a mesma decisao de acesso.

### Queue

Recursos vinculados a visitId validam o ator atraves da cadeia:

Queue -> Patient Journey -> Patient Registry

Paciente ou representante nao pode consultar ou alterar uma fila pertencente a outra visita.

### Presence

Eventos e historico vinculados a uma visita validam ownership antes da persistencia ou leitura.

O patientId informado tambem precisa corresponder ao paciente da visita.

### Notification

O historico e protegido por acesso ao paciente.

Preferencias de comunicacao nao possuem mais endpoint publico de escrita no Notification.

### Triage

O professionalId da decisao clinica e derivado do sub do JWT.

O cliente informa apenas a prioridade escolhida.

Isso impede spoofing da identidade profissional e preserva a rastreabilidade no evento clinical-priority-confirmed.

## Ownership do perfil de comunicacao

A v0.4 possuia responsabilidade sobreposta entre Identity, Patient Registry e Notification.

Na v0.4.1 o modelo foi consolidado:

Patient Registry -> patient-communication-profile-updated -> Kafka -> Notification

Responsabilidades:

- Patient Registry: source of truth.
- Notification: projecao operacional para escolha do canal.
- Identity: identidade do ator autenticado, sem ownership do estado de smartphone do paciente.

O fluxo foi validado em runtime pelo mesmo eventId entre Outbox do Registry e processed_events do Notification.

## Contratos

Os contratos OpenAPI e AsyncAPI foram realinhados com o runtime.

Principais correcoes:

- DecisionRequest nao contem mais professionalId;
- Identity expoe /api/v1/identity/me;
- APIs legadas de communication profile foram removidas do Identity;
- escrita direta de Notification Preference foi removida;
- Patient Registry documenta access, self-link e communication-profile;
- SELF foi removido do endpoint generico de criacao de representante;
- AsyncAPI inclui patient-communication-profile-updated;
- evento biometrico foi alinhado ao payload atualmente publicado.

## Consistencia HTTP

Nos pontos ajustados foram adicionados tratamentos explicitos para:

- 400 Bad Request para JSON invalido;
- 403 Forbidden para autorizacao negada;
- 404 Not Found para rota ou recurso inexistente;
- 405 Method Not Allowed para metodo HTTP incompativel.

## Validacoes ja realizadas

Durante a estabilizacao foram executados:

- testes Maven por modulo;
- testes de arquitetura;
- testes de autorizacao positiva e negativa;
- Docker build e recreate seletivo;
- health checks;
- chamadas reais ao Keycloak;
- validacoes em PostgreSQL;
- validacao de Outbox;
- processamento Kafka;
- validacao de projecao no Notification;
- checagem de restart count dos containers.

## Encerramento da v0.4.1

A regressao integral da v0.4.1 foi concluida.

A validacao final incluiu:

- reactor Maven completo;
- Testcontainers com PostgreSQL real;
- testes unitarios;
- testes de autorizacao;
- ArchUnit;
- stack Docker completa;
- health dos 11 deployables;
- validacao de restart count dos containers;
- E2E principal;
- E2E de representacao;
- E2E de paciente sem smartphone.

Durante a regressao da stack foi identificado um defeito de composicao no Queue Service.

QueueController dependia de QueueAccessApplicationService, mas a classe nao estava registrada como bean Spring.

A classe foi corrigida com @Service.

Depois da correcao:

- os 7 testes do Queue passaram;
- a anotacao foi confirmada no bytecode compilado;
- a imagem Docker do Queue foi reconstruida;
- o container permaneceu healthy;
- RestartCount permaneceu em zero;
- os E2Es passaram;
- o reactor Maven completo voltou a passar.

A v0.4.1 esta, portanto, encerrada como baseline estavel para inicio do Stage 1.
## Ainda fora do escopo validado

- modelo de IA clinicamente validado;
- integracao real com SAMU;
- HL7/FHIR;
- MQTT;
- hardware medico real;
- push/SMS de producao;
- EventStoreDB como adapter definitivo;
- implantacao produtiva em Kubernetes/Azure.

## Proxima evolucao funcional

Apos a conclusao da etapa 0.10, a proxima evolucao sera o Facility Service, responsavel inicialmente por:

- HealthFacility;
- CareZone;
- Bed;
- BedOccupation.

Nenhum novo bounded context deve ser introduzido antes da regressao completa da v0.4.1.
