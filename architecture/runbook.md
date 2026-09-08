# Runbook local — v0.4

## Infraestrutura somente
1. `docker compose up -d`.
2. Execute `mvn -B -ntp verify`.
3. Suba os serviços pela IDE ou `mvn -pl <modulo> spring-boot:run`.
4. Gateway `:8080`, Keycloak `:8180`, Clinical Query `:8090`, Grafana `:3000`, Tempo `:3200`.

## Stack completa
1. `./scripts/build-all.sh` ou `scripts\build-all.ps1`.
2. `docker compose -f docker-compose.full.yml up -d --build`.
3. Execute `scripts/e2e-demo.ps1` (Windows) ou `scripts/e2e-demo.sh` (Linux/macOS).

## Diagnóstico
- Actuator: `/actuator/health`, `/actuator/prometheus`.
- Kafka DLT: falhas após retries vão para `<topico>.DLT` nos consumers configurados.
- Correlation: envie `X-Correlation-Id`; eventos carregam `correlationId` e `traceId`.
- IA: `AI_PROVIDER=demo` prova somente o boundary. `AI_PROVIDER=http` usa `AI_BASE_URL` + Resilience4j.
- CQRS: aguarde consistência eventual antes de concluir que uma projeção está ausente; o script E2E faz polling.

## Segurança local
Usuários de demonstração e secrets do realm são apenas para desenvolvimento. Nunca reutilizar no deploy real.
