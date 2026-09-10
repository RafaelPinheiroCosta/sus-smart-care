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

## Status final

A v0.4.1 concluiu todas as etapas de estabilizacao planejadas:

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

## Regressao final

O reactor Maven completo foi executado sobre a arvore final de codigo.

Resultado:

- 12 modulos SUCCESS;
- BUILD SUCCESS;
- testes unitarios verdes;
- testes de autorizacao verdes;
- ArchUnit verde;
- Testcontainers/PostgreSQL verde.

Os erros SQLState 23505 produzidos pelo PatientRegistryPersistenceTest representam testes negativos intencionais de unicidade e nao falhas da suite.

Resultado final do Patient Registry:

- 24 testes;
- 0 failures;
- 0 errors.

Resultado final do Queue:

- 7 testes;
- 0 failures;
- 0 errors.

## Defeito encontrado durante a regressao

Na primeira validacao da stack completa o Queue Service entrou em restart loop.

A causa foi identificada como ausencia do registro Spring de:

QueueAccessApplicationService

QueueController dependia dessa classe por injecao de dependencia, mas ela nao estava registrada como bean.

A correcao aplicada foi:

- import org.springframework.stereotype.Service;
- anotacao @Service em QueueAccessApplicationService.

A correcao foi validada por:

- testes Maven do Queue;
- inspecao do bytecode com javap;
- rebuild da imagem Docker;
- recreate isolado do Queue;
- health HTTP 200;
- teste de estabilidade;
- RestartCount igual a zero;
- E2Es subsequentes;
- novo Maven reactor completo.

## Stack final

Foram validados simultaneamente os seguintes deployables:

- api-gateway;
- patient-registry-service;
- patient-journey-service;
- triage-service;
- queue-service;
- telemetry-service;
- presence-service;
- prehospital-service;
- notification-service;
- identity-access-service;
- clinical-query-service.

Resultado:

- 11 de 11 health checks HTTP 200;
- todos em estado running;
- nenhum restart inesperado.

A verificacao final da stack completa mostrou RestartCount igual a zero para todos os containers do projeto.

O PostgreSQL apresentou 21 conexoes durante a verificacao final, permanecendo distante do limite que anteriormente havia causado saturacao.

## E2E principal

O fluxo principal foi concluido com exit code 0.

Foram validados:

- autenticacao;
- Patient canonico;
- pre-atendimento;
- anamnese;
- check-in;
- criacao da triagem por evento;
- dispositivo e sessao de telemetria;
- biometria normalizada;
- boundary de IA;
- confirmacao humana da prioridade;
- entrada automatica na fila;
- presenca, saida e retorno;
- Clinical Query CQRS;
- tela publica sem dados pessoais.

A prioridade clinica utilizada no cenario foi HIGH.

## E2E de representacao

O fluxo foi concluido com exit code 0.

Foi comprovado que uma alteracao de representacao nao altera o Patient canonico nem o historico associado ao PatientId.

## E2E sem smartphone

O fluxo foi concluido com exit code 0.

Foram comprovados:

- tela publica sem PII;
- senha operacional;
- saida assistida;
- janela de retorno;
- notificacao por DISPLAY;
- notificacao por STAFF_ASSISTED;
- retorno registrado pela recepcao;
- manutencao da prioridade clinica MEDIUM.

Esse cenario confirma o principio digital-first, not digital-only.

## Conclusao

A v0.4.1 esta validada como baseline estavel.

Os criterios de encerramento foram atendidos:

- build completo verde;
- testes automatizados verdes;
- stack completa operacional;
- autorizacao fine-grained validada;
- integracao orientada a eventos validada;
- E2Es principais verdes;
- containers estaveis;
- documentacao alinhada ao runtime.

A proxima evolucao funcional e o Stage 1 - Facility Service.
