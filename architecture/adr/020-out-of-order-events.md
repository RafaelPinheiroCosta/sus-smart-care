# ADR-020 — Eventos entre tópicos podem chegar fora de ordem
**Decisão:** consumidores não assumem ordenação global do Kafka. Queue guarda prioridade clínica pendente até receber o mapeamento Visit→Facility; Notification guarda chamadas pendentes até receber Visit→Patient.
**Motivo:** Kafka só garante ordenação dentro de uma partição/tópico. Fluxos compostos por tópicos diferentes precisam ser tolerantes a reordenação.
