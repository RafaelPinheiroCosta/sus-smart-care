# Testes, execução e observabilidade

## 1. Perfis de execução

### Infraestrutura

```bash
docker compose up -d
```

Esse compose sobe a infraestrutura compartilhada para desenvolvimento dos módulos.

### Stack completa

PowerShell:

```powershell
.\scripts\run-full-stack.ps1
```

Bash:

```bash
./scripts/run-full-stack.sh
```

O compose completo sobe as 12 aplicações e os componentes de infraestrutura usados na demonstração.

## 2. Portas das aplicações

| Aplicação | Porta |
|---|---:|
| API Gateway | 8080 |
| Patient Registry | 8081 |
| Patient Journey | 8082 |
| Triage | 8083 |
| Queue | 8084 |
| Telemetry | 8085 |
| Presence | 8086 |
| Pre-Hospital | 8087 |
| Notification | 8088 |
| Identity Access | 8089 |
| Clinical Query | 8090 |
| Facility | 8091 |

Infraestrutura principal: Keycloak `8180`, PostgreSQL `5432`, Redis `6379`, Kafka `9092`, Mosquitto `1883`, Prometheus `9090`, Grafana `3000` e Tempo `3200/4317/4318`.

## 3. Estratégia de testes

- **Domínio:** JUnit para invariantes e máquinas de estado.
- **Arquitetura:** ArchUnit para preservar limites entre domínio, aplicação, adapters e infraestrutura.
- **Persistência/integração:** Testcontainers e bancos reais nos testes adequados.
- **Contratos:** validação estrutural de OpenAPI e AsyncAPI.
- **Segurança:** cenários positivos/negativos por role e ownership.
- **End-to-end:** scripts PowerShell/Bash percorrendo a jornada distribuída.
- **Carga:** k6 para endpoints de leitura e ingestão de telemetria.

## 4. Cenários E2E disponíveis

- `scripts/e2e-demo.ps1`: jornada principal até ClinicalPatientView.
- `scripts/e2e-no-smartphone.ps1`: atendimento assistido, fila, saída/retorno e telão anonimizado.
- `scripts/e2e-representation.ps1`: responsável e continuidade do mesmo Patient.
- `scripts/e2e-ambulance.ps1`: ambulância, pré-hospitalar e telemetria antes da chegada.
- `scripts/security-smoke.ps1`: autorização e endpoints públicos/protegidos.

Os fluxos que dependem de Kafka/CQRS aguardam consistência eventual por polling, evitando concluir falha apenas por atraso da projeção.

## 5. Validação final registrada no desenvolvimento

A rodada final do Stage 8 registrou sucesso para:

- `FINAL HACKATHON E2E`;
- health das 12 aplicações;
- 12 containers de aplicação;
- 12 targets Prometheus;
- serialização/locking da projeção CQRS;
- tolerância a eventos entre tópicos fora de ordem;
- projeção monotônica da jornada;
- Flyway/Clinical Query;
- hierarquia do Facility Service.

Esses resultados representam o ambiente local validado durante o desenvolvimento. O CI continua sendo a verificação reproduzível após o push.

## 6. Validação antes de publicar

```powershell
python scripts/static_validate.py
python scripts/validate-structure.py
python scripts/validate_contracts.py
python scripts/release_validate.py
mvn -B -ntp verify
```

Para validar a stack real, execute também o `docker-compose.full.yml`, confira `/actuator/health` das aplicações e rode os E2Es relevantes.

## 7. Observabilidade

Os serviços expõem Actuator/Prometheus. OpenTelemetry envia traces para Tempo e Grafana consulta as fontes configuradas no ambiente local. `X-Correlation-Id`, `correlationId` e `traceId` ajudam a seguir uma jornada através de REST e eventos.

Redis indisponível não elimina a cópia persistente do Clinical Query. Falha da IA não bloqueia triagem. Falhas de mensageria usam retry/DLT nos consumers configurados.
