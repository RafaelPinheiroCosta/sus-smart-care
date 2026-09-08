# Fluxo de desenvolvimento
1. Event Storming → eventos, comandos, agregados e bounded contexts.
2. ADR antes de tecnologia com impacto distribuído.
3. OpenAPI/AsyncAPI antes de integração.
4. Vertical slice pequeno e funcional.
5. Regra de domínio + teste unitário.
6. Persistência/migração Flyway.
7. Outbox para eventos de integração; consumidor idempotente e DLT quando crítico.
8. Projection/View Data quando leitura exige fan-out ou baixa latência.
9. Resiliência e fallback antes de integrar dependência não crítica.
10. Observabilidade (correlation ID, logs, métricas, traces).
11. Testes integração/contrato/carga.
12. Container → Kubernetes → IaC/Cloud.
