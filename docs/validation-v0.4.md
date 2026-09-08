# Validação técnica — v0.4

## Executável neste ambiente de geração
- parsing XML/POM;
- parsing YAML/OpenAPI/AsyncAPI/Compose;
- validação estática de contratos;
- verificação estrutural de migrations;
- varredura sintática Java com `javac` sem classpath de dependências.

## Não disponível neste ambiente
- Maven completo;
- Docker/Compose;
- Testcontainers em execução;
- chamadas reais ao Keycloak/Kafka/PostgreSQL.

Por isso, a v0.4 não declara falsamente um build verde. O CI e os scripts de E2E são a fonte de validação executável quando o projeto for aberto em ambiente com Maven e Docker.

## Smoke test executado
- compilação Java 21 dos agregados puros `Visit` e `Triage`;
- criação de eventos, `pullChanges()` e reidratação;
- resultado: `domain-smoke-v0.4-ok`.

A varredura de 214 arquivos Java não encontrou erros sintáticos; erros do `javac` sem classpath são exclusivamente referências às dependências externas não disponíveis neste runtime.
