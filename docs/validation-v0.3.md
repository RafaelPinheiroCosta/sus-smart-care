# Validação técnica — v0.3

Data da validação: 2026-09-07.

## Executado neste ambiente

- parsing XML de todos os `pom.xml`: **12 arquivos / 0 erros**;
- parsing de todos os documentos YAML: **39 arquivos / 0 erros**;
- validação estrutural dos contratos OpenAPI/AsyncAPI pelos scripts do projeto: **OK**;
- verificação de versões Flyway duplicadas e invariantes técnicas do Outbox: **OK**;
- verificação de que produtores Outbox usam payload String compatível com o envelope versionado: **OK**;
- varredura `javac` em **213 fontes Java** para localizar problemas estruturais independentes do classpath externo: **0 candidatos estruturais**;
- smoke test Java puro do replay Event Sourcing de `Visit` e `Triage`: **domain-smoke-ok**;
- busca por referências residuais à versão 0.2: coleção Postman atualizada para v0.3.

## Correções encontradas durante a validação

A varredura estática detectou duas inferências `var` ambíguas no Clinical Query. Elas foram substituídas por tipo explícito. A View Data clínica também foi fortalecida para manter **a observação mais recente por tipo biométrico**, em vez de somente a última observação global.

## Limitação do ambiente

`mvn verify`, Testcontainers e Docker Compose não puderam ser executados neste runtime porque Maven/Docker não estão instalados e o ambiente não possui acesso externo para baixar Maven/dependências. Portanto não afirmamos que a resolução completa de dependências/contexto Spring foi executada localmente.

A pipeline `.github/workflows/ci.yml` e os scripts `scripts/check-project.sh` / `scripts/check-project.ps1` foram preparados para realizar a validação completa assim que o projeto estiver em um ambiente com Maven e Docker.
