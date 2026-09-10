# patient-journey-service

Porta local: `8082`.

Estrutura alvo: domain / application / adapters / infrastructure.

## Emergency patient journey

The Patient Journey service owns the macro lifecycle of an emergency visit.

The event-sourced lifecycle now supports:

PRE_ARRIVAL
-> WAITING_TRIAGE
-> IN_TRIAGE
-> TRIAGED
-> QUEUED
-> CALLED
-> IN_SERVICE

A patient may then:

- finish with DISCHARGED;
- finish with TRANSFERRED;
- enter WAITING_NEXT_STAGE and repeat CALLED -> IN_SERVICE;
- terminate as CANCELLED when operationally appropriate.

Patient Journey does not calculate clinical priority. The Triage service remains
authoritative for clinical priority.

Patient Journey does not calculate queue order or waiting time. The Queue
service remains authoritative for ordering and queue policy.

Every macro transition is persisted in the Visit event stream and also emits an
outbox integration event.

A COMPLETED or CANCELLED journey is terminal and cannot be moved back into an
active care state.