# Choreography E2E — paciente digital

```mermaid
sequenceDiagram
    actor P as Paciente/App
    participant G as API Gateway
    participant R as Patient Registry
    participant J as Patient Journey
    participant K as Kafka
    participant T as Triage
    participant D as Telemetry
    participant Q as Queue
    participant C as Clinical Query

    P->>G: registrar/localizar Patient
    G->>R: POST /patients
    P->>G: criar pré-visita + anamnese
    G->>J: POST /pre-visits
    J-->>K: pre-visit-created
    P->>G: check-in
    G->>J: POST /check-in
    J-->>K: patient-checked-in
    K-->>T: cria Triage idempotentemente
    D-->>K: biometric-observation-received
    K-->>T: atualiza projeção biométrica
    T-->>K: triage-assessment-generated
    T-->>K: clinical-priority-confirmed
    K-->>Q: cria QueueEntry quando facility e prioridade convergem
    Q-->>K: queue-position-estimated
    K-->>C: projeta ClinicalPatientView
    P->>G: consulta status/telão
```

A demo (`scripts/e2e-demo.ps1`) consulta endpoints de lookup e faz **polling de consistência eventual**, em vez de depender de sleeps fixos para Triage, Queue e Clinical View.
