# SUS Smart Care

Backend distribuído para acompanhamento da jornada do paciente no SUS, com foco em previsibilidade da espera, continuidade clínica, inclusão de pacientes sem smartphone, suporte ao fluxo pré-hospitalar e integração de telemetria.

O projeto foi modelado a partir de Event Storming e segue o princípio **digital-first, not digital-only**: recursos digitais reduzem atrito, mas não condicionam o atendimento e não alteram a prioridade clínica definida pelos profissionais de saúde.

## O que está implementado

A solução final do MVP possui **12 aplicações Spring** no monorepo:

| Aplicação | Porta | Responsabilidade principal |
|---|---:|---|
| API Gateway | 8080 | entrada única e roteamento |
| Patient Registry | 8081 | identidade clínica canônica, identificadores e representação |
| Patient Journey | 8082 | pré-visita, check-in e ciclo da visita |
| Triage | 8083 | triagem, apoio de IA e confirmação profissional da prioridade |
| Queue | 8084 | fila, posição estimada, chamada e política de retorno |
| Telemetry | 8085 | dispositivos, MQTT, sessões e telemetria biométrica |
| Presence | 8086 | presença, tracking e transição entre zonas |
| Pre-Hospital | 8087 | ambulâncias, cobertura, ETA e atendimento pré-hospitalar |
| Notification | 8088 | roteamento e histórico de comunicação |
| Identity Access | 8089 | apoio à identidade/autorização integrada ao Keycloak |
| Clinical Query | 8090 | read side CQRS e `ClinicalPatientView` |
| Facility | 8091 | unidades, zonas de cuidado, leitos e ocupações |

### Jornada suportada

1. consulta prévia da unidade e da fila;
2. criação ou localização do `Patient`, inclusive identidade provisória;
3. pré-visita e pré-anamnese;
4. check-in presencial;
5. triagem e ingestão de biometria;
6. apoio de regras/IA, com decisão final humana;
7. entrada e acompanhamento da fila clínica;
8. saída temporária, retorno, tolerância e readiness operacional;
9. chamada e atendimento em múltiplas etapas;
10. alta, transferência ou cancelamento conforme o lifecycle da visita;
11. visão clínica consolidada por CQRS;
12. fluxo pré-hospitalar com ambulância, ETA e telemetria antes da chegada.

## Decisões centrais

- `Patient != UserAccount`: o histórico pertence ao paciente, não à conta nem ao representante.
- Representação é temporal, revogável e auditável.
- Smartphone, canal de entrada e pré-cadastro não concedem prioridade clínica.
- Prioridade clínica e posição/readiness operacional são conceitos separados.
- IA é apoio à decisão; falha do provider não bloqueia a triagem humana.
- REST atende comandos/consultas síncronos; Kafka é o backbone de eventos internos.
- MQTT é o boundary de ingestão de telemetria de dispositivos no MVP local.
- Transactional Outbox, envelope versionado, idempotência e DLT reduzem inconsistências em falhas parciais.
- Event Sourcing é seletivo em Journey e Triage; os streams do MVP são persistidos em PostgreSQL.
- Clinical Query usa PostgreSQL como read model persistente e Redis como leitura quente.

## Infraestrutura local

A stack completa usa PostgreSQL, Kafka, Redis, Keycloak, Eclipse Mosquitto, Prometheus, Tempo e Grafana. EventStoreDB permanece no ambiente como componente de referência para evolução do adapter; o runtime atual não depende dele para reconstruir Journey/Triage.

### Subir somente a infraestrutura

```bash
docker compose up -d
```

### Subir a stack completa

PowerShell:

```powershell
.\scripts\run-full-stack.ps1
```

Bash:

```bash
./scripts/run-full-stack.sh
```

Acessos principais:

- Gateway: `http://localhost:8080`
- Keycloak: `http://localhost:8180`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`

## Usuários de demonstração

- `demo.admin / admin123`
- `demo.patient / demo123`
- `demo.representative / demo123`
- `demo.triage / demo123`
- `demo.doctor / demo123`
- `demo.operator / demo123`
- `demo.ambulance / demo123`

O client M2M `device-client` e as credenciais MQTT existentes no repositório são **somente para desenvolvimento local**.

## Validação

Com a stack completa em execução, o cenário principal pode ser demonstrado com:

```powershell
.\scripts\e2e-demo.ps1
```

Também existem cenários específicos para ambulância, paciente sem smartphone, representação e segurança. Para validação estática/contratos:

```powershell
python scripts/static_validate.py
python scripts/validate-structure.py
python scripts/validate_contracts.py
python scripts/release_validate.py
```

E para o reactor Maven:

```bash
mvn -B -ntp verify
```

## Documentação

A documentação principal foi consolidada para evitar versões concorrentes da mesma informação:

- [`docs/architecture.md`](docs/architecture.md): arquitetura, serviços, integração e decisões de implementação.
- [`docs/security-and-data.md`](docs/security-and-data.md): identidade, autorização, privacidade e governança de dados.
- [`docs/testing-and-operations.md`](docs/testing-and-operations.md): execução, observabilidade, testes e cenários E2E.
- [`architecture/adr/`](architecture/adr/): decisões arquiteturais que explicam o porquê das escolhas.
- [`contracts/`](contracts/): OpenAPI e AsyncAPI; fonte autoritativa dos contratos HTTP e de mensageria.

Diagramas Mermaid ficam em [`architecture/diagrams/`](architecture/diagrams/) e o registro visual do Event Storming em [`architecture/event-storming/`](architecture/event-storming/).

## Limites atuais

- `AI_PROVIDER=demo` comprova o boundary de integração, mas não é um modelo clínico validado.
- MQTT está implementado no ambiente local com Mosquitto; integração com hardware médico real exige credenciais, TLS/ACL e validação do fabricante/protocolo.
- Presence já modela sinais `BLE`, `WIFI`, `UWB`, `QR`, `KIOSK`, `MANUAL` e `SYSTEM`, porém a descoberta automática de celulares/dispositivos por Wi-Fi ou Bluetooth ainda não faz parte do MVP.
- Integrações reais com SAMU, prontuários externos e HL7/FHIR permanecem como evolução.
- Kubernetes e Bicep são referências de implantação, não evidência de produção já operando em cloud.
