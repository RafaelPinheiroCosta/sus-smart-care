# Contratos da plataforma

- `openapi/`: contratos HTTP externos, versionados em `v1`.
- `asyncapi/platform-events.yaml`: catálogo dos tópicos/eventos Kafka e envelope de integração.

Na v0.4 os contratos são a **fonte documental autoritativa** e passam por validação estrutural no CI. A geração automática de interfaces/DTOs permanece propositalmente bloqueada até o primeiro `mvn verify` completo ficar verde, evitando introduzir código gerado não validado no baseline.

Regras de evolução:
1. alteração breaking exige nova versão de API/event schema;
2. eventos carregam `schemaVersion`;
3. campos novos devem ser opcionais quando possível;
4. `correlationId` e `traceId` devem atravessar integrações;
5. dados sensíveis não devem ser incluídos em eventos sem necessidade de negócio.
