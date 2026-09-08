# Próximas ações — depois da v0.4

A v0.4 deliberadamente encerra a fase de acrescentar arquitetura antes da prova executável. Próxima prioridade:

1. Rodar `mvn -B -ntp verify` em ambiente com Maven/JDK 21.
2. Corrigir qualquer erro real de compilação/dependência detectado.
3. Rodar `docker compose -f docker-compose.full.yml up -d --build`.
4. Executar `scripts/e2e-demo.ps1` e registrar evidências.
5. Executar cenário de ambulância/telemetria e paciente sem smartphone.
6. Validar dashboards/traces no Grafana/Tempo.
7. Só depois introduzir geração OpenAPI (interfaces/DTOs), fine-grained authorization e adapter EventStoreDB.
8. Em seguida fechar Kubernetes/Azure/Bicep e materiais do Hackathon.
