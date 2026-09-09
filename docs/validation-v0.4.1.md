# Validacao tecnica - v0.4.1

## Objetivo

Este documento registra as evidencias da estabilizacao realizada sobre a baseline v0.4.

O arquivo validation-v0.4.md permanece preservado como registro historico da condicao original em que a baseline foi criada.

## Ambiente utilizado

- Windows PowerShell 5.1
- Java 21
- Maven 3.9.x
- Docker Desktop
- PostgreSQL
- Kafka
- Redis
- Keycloak
- Testcontainers

## Correcoes validadas

### CI e E2E

- branch do workflow ajustada para master;
- cenario de representacao atualizado para utilizar self-link;
- scripts de Triage deixaram de enviar professionalId.

### Queue

Foi validado que:

- paciente acessa a propria visita;
- paciente nao acessa visita estrangeira;
- representante sem vinculo recebe acesso negado;
- operador autorizado consegue consultar;
- visualizacao publica permanece disponivel;
- operacoes nao autorizadas nao alteram o banco.

### Presence

Foi validado que:

- paciente acessa o proprio historico;
- paciente nao acessa visita estrangeira;
- representante sem vinculo nao acessa;
- operador autorizado acessa;
- patientId incompativel com a visita e rejeitado;
- tentativa nao autorizada nao persiste evento.

### Notification

Foi validado que:

- paciente consulta seu proprio historico;
- paciente nao consulta historico estrangeiro;
- representante sem vinculo nao consulta;
- operador autorizado consulta;
- alteracao nao autorizada de preferencia foi bloqueada.

Posteriormente, o endpoint publico de escrita da preferencia foi removido porque o Notification deixou de ser source of truth.

### Triage

Foi validado que:

- o request envia apenas priority;
- professionalId nao e recebido do cliente;
- decidedBy e exatamente o sub do JWT do profissional autenticado;
- o container permaneceu sem restart inesperado.

### Communication Profile Ownership

Foi validado o fluxo:

Patient Registry -> Transactional Outbox -> Kafka -> Notification

Evidencias observadas:

- evento patient-communication-profile-updated marcado como publicado no Outbox;
- mesmo eventId registrado em processed_events pelo Notification;
- projecao criada como false|DISPLAY|STAFF_ASSISTED.

Isso confirma que o Patient Registry e a fonte de verdade e o Notification mantem apenas uma projecao.

### Contratos

Foram alinhados:

- Triage OpenAPI;
- Identity OpenAPI;
- Notification OpenAPI;
- Patient Registry OpenAPI;
- plataforma AsyncAPI;
- colecao Postman.

### Semantica HTTP

Foram validados em runtime:

- rota removida no Identity: 404;
- metodo incompativel: 405;
- rota inexistente no Notification: 404;
- endpoint de escrita removido bloqueado pelo Security: 403;
- JSON malformado: 400.

## Testes incrementais

Os modulos afetados por cada alteracao foram compilados e testados antes dos respectivos checkpoints.

Os servicos alterados foram reconstruidos e recriados seletivamente no Docker.

Health checks e restart counts foram verificados apos os recreates.

## Status atual

Etapas concluidas:

- 0.1 CI;
- 0.2 E2E representation;
- 0.3 Queue authorization;
- 0.4 Presence authorization;
- 0.5 Notification authorization;
- 0.6 Triage professional identity;
- 0.7 communication profile ownership;
- 0.8 contracts and HTTP semantics;
- 0.9 documentation.

## Validacao final pendente

Ainda falta a etapa 0.10.

Ela executara a regressao completa apos todas as alteracoes da v0.4.1.

Comando principal:

mvn clean verify

Depois devem ser executados:

- stack completa;
- health checks;
- E2E principal;
- E2E de representacao;
- E2E de paciente sem smartphone;
- verificacao final dos containers.

Apos a etapa 0.10, este documento devera receber o resultado final da regressao e o commit correspondente sera o checkpoint de encerramento da v0.4.1.
