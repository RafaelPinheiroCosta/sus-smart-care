# ClinicalPatientView — View Data para o profissional

`ClinicalPatientView` é o **read model CQRS** da tela futura do médico/profissional. Ele não é fonte de verdade clínica e não executa fan-out síncrono durante a consulta.

## Composição

A projeção é atualizada por eventos de vários contextos:

```text
Patient Registry ── patient-profile-* ─────┐
Patient Journey ─── pre-visit/check-in ────┤
Telemetry ───────── biometric-observation ─┤
Triage ──────────── assessment/priority ───┼──> Projection Consumer
Presence ────────── presence-changed ──────┤          │
Queue ───────────── position-estimated ────┤          ▼
Pre-Hospital ────── pre-arrival-alert ─────┘   PostgreSQL Read Model
                                                     │
                                                     ▼
                                                   Redis
                                                     │
                                                     ▼
                                            Clinical Query API
```

## Biometria

A view mantém a **última observação por tipo**, e não somente a última medição global. Assim uma consulta pode receber simultaneamente, por exemplo, última saturação, frequência cardíaca, temperatura e pressão quando esses tipos tiverem sido publicados.

Esses dados são uma projeção de leitura. Telemetry continua dono das observações recebidas e Triage continua dono da decisão clínica.

## Consistência

A view é eventualmente consistente. O contrato deve exibir `updatedAt`; no futuro pode também expor `projectionLag` quando houver métricas suficientes. Decisões críticas devem ser confirmadas no fluxo transacional apropriado e não depender exclusivamente de uma projeção possivelmente atrasada.

## Cache

Redis é uma otimização. A cópia persistente em PostgreSQL permite fallback caso o cache esteja indisponível. TTL atual: 10 minutos, atualizado a cada evento que altera a view.
