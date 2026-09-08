# SUS Smart Care — v0.4

Backend/arquitetura de uma plataforma inteligente de jornada do paciente para o SUS. O projeto é **digital-first, not digital-only**: smartphone e pré-cadastro reduzem atrito, mas nunca criam prioridade clínica nem condicionam o direito ao atendimento.

## Jornada coberta
1. consulta prévia da unidade/fila estimada;
2. paciente, responsável, totem, recepção ou ambulância cria/localiza o `Patient` canônico;
3. pré-visita e pré-anamnese antes da chegada quando possível;
4. check-in presencial;
5. triagem + telemetria biométrica normalizada;
6. regras/IA como apoio e decisão final humana;
7. entrada automática na fila segundo prioridade clínica;
8. presença/saída/retorno e tolerância operacional;
9. View Data específica para médico/paciente/telão;
10. fluxo pré-hospitalar com ambulância, ETA e telemetria antes da chegada.

## Bounded contexts
`Identity & Access`, `Patient Registry`, `Patient Journey`, `Triage`, `Telemetry & Device Integration`, `Queue Management`, `Presence`, `Pre-Hospital` e `Notification`. `Clinical Query Service` é o read side técnico de CQRS, não um novo domínio de negócio.

## Stack
Java 21, Spring Boot 3, Spring Cloud Gateway, Spring Security OAuth2/OIDC/JWT, PostgreSQL/Flyway, Kafka, Redis, Event Sourcing seletivo, Resilience4j, OpenAPI/AsyncAPI, Micrometer/OpenTelemetry, Prometheus/Tempo/Grafana, Docker, Kubernetes/Bicep preparados, JUnit/Testcontainers/ArchUnit/k6 e GitHub Actions.

## Regras de domínio invariantes
- `Patient != UserAccount`; histórico clínico pertence ao paciente.
- responsável/tutor é vínculo temporal/revogável, não proprietário do histórico.
- paciente pode existir sem conta, documento confirmado ou smartphone.
- pré-cadastro/canal de entrada não altera prioridade.
- sair/atrasar pode alterar posição operacional/readiness, nunca prioridade clínica automaticamente.
- telemetria pré-hospitalar antecipa preparação e avaliação, mas não substitui validação clínica.
- IA é apoio versionado/explicável e falha de IA não pode bloquear triagem humana.

## Executar apenas a infraestrutura
```bash
docker compose up -d
```
Depois execute os módulos pela IDE/Maven.

## Executar stack completa
É necessário gerar os JARs primeiro:
```powershell
.\scripts\run-full-stack.ps1
```
ou:
```bash
./scripts/run-full-stack.sh
```

Gateway `http://localhost:8080`, Keycloak `http://localhost:8180`, Grafana `http://localhost:3000`.

## Usuários locais de demonstração
- `demo.admin / admin123`
- `demo.patient / demo123`
- `demo.representative / demo123`
- `demo.triage / demo123`
- `demo.doctor / demo123`
- `demo.operator / demo123`
- `demo.ambulance / demo123`

O client M2M `device-client` usa `client_credentials` e secret `device-secret` **somente no ambiente local de demonstração**.

## E2E
Com a stack completa em execução:
```powershell
.\scripts\e2e-demo.ps1
```
O script percorre `Patient → PreVisit → CheckIn → Triage → Telemetry → AI boundary → decisão humana → Queue → Presence → ClinicalPatientView` e faz polling das projeções eventualmente consistentes.

## Contratos
- `contracts/openapi/*.yaml`: boundaries HTTP.
- `contracts/asyncapi/platform-events.yaml`: tópicos/eventos Kafka.
- `contracts/README.md`: política de evolução.

## Segurança clínica
`AI_PROVIDER=demo` não é modelo médico. Ele prova somente o boundary arquitetural. Qualquer uso clínico real exige protocolo institucional, validação do modelo, governança e supervisão profissional.

## Estado
Veja `architecture/implementation-status.md` e `docs/validation-v0.4.md`. Esta versão foi validada estruturalmente, mas o ambiente de geração não possui Maven/Docker para afirmar `mvn verify`/full-stack verdes.
