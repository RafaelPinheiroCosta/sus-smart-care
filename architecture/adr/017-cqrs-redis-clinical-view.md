# ADR-017 — Clinical View persistente + Redis
**Decisão:** `Clinical Query Service` mantém projeção persistente em PostgreSQL e uma cópia quente com TTL no Redis. A API tenta Redis e faz fallback transparente para PostgreSQL.
**Motivo:** médicos e painéis precisam de leitura consolidada e baixa latência sem fan-out síncrono para diversos microsserviços.
