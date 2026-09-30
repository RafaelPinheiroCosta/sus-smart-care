# Contratos da plataforma

Esta pasta é a fonte documental autoritativa dos boundaries de integração.

- `openapi/`: contratos HTTP externos em `/api/v1/**`.
- `asyncapi/platform-events.yaml`: eventos internos Kafka e envelope de integração.
- `asyncapi/telemetry-mqtt.yaml`: contrato do boundary MQTT para dispositivos.

Cada contrato possui sua própria versão de evolução; a versão declarada em `info.version` não precisa ser igual à versão do artefato Maven. Mudanças breaking exigem nova versão do contrato/API/event schema.

Regras:

1. eventos carregam `schemaVersion`;
2. consumidores devem tolerar adição de campos opcionais quando possível;
3. `correlationId` e `traceId` atravessam integrações relevantes;
4. dados sensíveis só aparecem quando necessários ao negócio;
5. MQTT de dispositivo não transporta `patientId`, `visitId`, CPF ou CNS;
6. contratos passam por validação estrutural no CI.
