# SUS Smart Care - v0.4.1

Backend e arquitetura de uma plataforma inteligente para a jornada do paciente no SUS.

O projeto segue o principio digital-first, not digital-only: smartphone, pre-cadastro e automacoes reduzem atrito, mas nunca condicionam o direito ao atendimento nem definem prioridade clinica.

## Jornada coberta

1. consulta previa da unidade e estimativa de fila;
2. paciente, responsavel, totem, recepcao ou ambulancia cria ou localiza o Patient canonico;
3. pre-visita e pre-anamnese antes da chegada quando possivel;
4. check-in presencial;
5. triagem e telemetria biometrica normalizada;
6. regras e IA como apoio a decisao;
7. decisao final de prioridade confirmada por profissional autenticado;
8. entrada na fila segundo prioridade clinica;
9. presenca, saida, retorno e tolerancia operacional;
10. notificacoes adaptadas ao perfil de comunicacao;
11. View Data consolidada para consumo clinico;
12. fluxo pre-hospitalar com ambulancia, ETA e telemetria antes da chegada.

## Bounded contexts

A plataforma possui os seguintes contextos de negocio:

- Identity & Access
- Patient Registry
- Patient Journey
- Triage
- Telemetry & Device Integration
- Queue Management
- Presence
- Pre-Hospital
- Notification

O Clinical Query Service funciona como read side tecnico de CQRS e nao representa um novo dominio de negocio.

O Facility Service esta planejado para a proxima etapa evolutiva e ainda nao pertence a v0.4.1.

## Stack

Java 21, Spring Boot 3, Spring Cloud Gateway, Spring Security OAuth2/OIDC/JWT, PostgreSQL, Flyway, Kafka, Redis, Event Sourcing seletivo, Resilience4j, OpenAPI, AsyncAPI, Micrometer, OpenTelemetry, Prometheus, Tempo, Grafana, Docker, JUnit, Testcontainers, ArchUnit, k6 e GitHub Actions.

## Regras de dominio invariantes

- Patient != UserAccount.
- O historico clinico pertence ao paciente.
- Responsavel, tutor ou familiar autorizado e vinculo temporal e revogavel.
- Um paciente pode existir sem conta, documento confirmado ou smartphone.
- Canal de entrada e pre-cadastro nao alteram prioridade clinica.
- Saida ou atraso podem alterar readiness ou posicao operacional, mas nunca a prioridade clinica automaticamente.
- Telemetria pre-hospitalar antecipa preparacao e avaliacao, mas nao substitui validacao profissional.
- IA e apoio a decisao e falha de IA nao pode bloquear triagem humana.
- A prioridade clinica final e uma decisao humana auditavel.
- O identificador do profissional que confirma a prioridade vem do sub do JWT autenticado e nao do corpo enviado pelo cliente.

## Ownership de dados

A v0.4.1 explicita a responsabilidade de cada contexto:

- Patient Registry: paciente canonico, identificadores, vinculos de representacao e perfil de comunicacao.
- Identity & Access / Keycloak: usuarios, autenticacao, papeis e identidade do ator autenticado.
- Patient Journey: ciclo da visita do paciente.
- Triage: avaliacao e prioridade clinica.
- Queue: ordenacao e estado da fila.
- Presence: presenca fisica e eventos de localizacao.
- Telemetry: dispositivos, sessoes e observacoes biometricas.
- Pre-Hospital: atendimento pre-hospitalar.
- Notification: entrega de notificacoes e projecao local das preferencias de comunicacao.
- Clinical Query: visao consolidada para leitura.

O perfil de comunicacao possui uma unica fonte de verdade no Patient Registry.

O Notification recebe patient-communication-profile-updated por Kafka e mantem apenas uma projecao para decisao de canal.

## Seguranca

A plataforma combina dois niveis de autorizacao:

1. coarse-grained authorization, baseada nas roles do JWT;
2. fine-grained authorization, baseada no recurso e no vinculo do usuario com o paciente ou visita.

Os principais fluxos protegidos incluem:

- acesso a paciente por vinculo SELF ou representacao ativa;
- leitura e alteracao de recursos de fila vinculados a visita;
- eventos e historico de Presence vinculados a visita;
- historico de Notification vinculado ao paciente;
- alteracao do perfil de comunicacao somente por ator autorizado;
- decisao clinica atribuida ao profissional autenticado pelo JWT.

As visualizacoes publicas de fila permanecem anonimizadas e nao exigem acesso ao prontuario.

## Executar infraestrutura

Comando:

docker compose up -d

## Executar stack completa

Primeiro gere os JARs e entao execute no PowerShell:

.\scripts\run-full-stack.ps1

Ou no Bash:

./scripts/run-full-stack.sh

Principais acessos locais:

- API Gateway: http://localhost:8080
- Keycloak: http://localhost:8180
- Grafana: http://localhost:3000

## Usuarios locais de demonstracao

- demo.admin / admin123
- demo.patient / demo123
- demo.representative / demo123
- demo.triage / demo123
- demo.doctor / demo123
- demo.operator / demo123
- demo.ambulance / demo123

O client M2M device-client usa client_credentials e o secret device-secret somente no ambiente local de demonstracao.

## E2E

Com a stack completa em execucao, execute:

.\scripts\e2e-demo.ps1

Tambem existem cenarios especificos para representacao e paciente sem smartphone.

Os scripts utilizam polling quando dependem de projecoes e consistencia eventual.

## Contratos

- contracts/openapi/*.yaml: boundaries HTTP.
- contracts/asyncapi/platform-events.yaml: catalogo de eventos Kafka.
- contracts/README.md: politica de evolucao.

Na v0.4.1 os contratos foram realinhados com o runtime, incluindo:

- identidade do profissional derivada do JWT;
- perfil de comunicacao no Patient Registry;
- remocao da escrita direta de preferencias no Notification;
- endpoint /identity/me;
- eventos de atualizacao do perfil de comunicacao.

## Seguranca clinica

AI_PROVIDER=demo nao representa um modelo medico validado.

Ele demonstra somente o boundary arquitetural para apoio a decisao. Uso clinico real exige protocolo institucional, validacao de modelo, governanca, auditoria e supervisao profissional.

## Estado da v0.4.1

As etapas incrementais de estabilizacao 0.1 a 0.8 foram executadas e validadas localmente com Maven, Docker, Keycloak, PostgreSQL e Kafka.

Entre as correcoes ja validadas estao:

- CI alinhado a branch correta;
- E2E de representacao atualizado;
- BOLA/IDOR corrigido em Queue;
- BOLA corrigido em Presence;
- BOLA corrigido em Notification;
- identidade profissional de Triage derivada do JWT;
- ownership de comunicacao centralizado no Patient Registry;
- projecao Registry -> Kafka -> Notification validada em runtime;
- contratos OpenAPI/AsyncAPI alinhados;
- semantica HTTP corrigida para 400, 403, 404 e 405 nos pontos ajustados.

A regressao completa da v0.4.1 ainda sera executada na etapa 0.10 antes da promocao desta baseline para a proxima fase funcional.

Veja tambem:

- architecture/implementation-status.md
- docs/validation-v0.4.1.md
