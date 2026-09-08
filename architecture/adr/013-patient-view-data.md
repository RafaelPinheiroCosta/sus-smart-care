# ADR-013 — View Data clínica

**Decisão:** médicos e demais perfis consomem projeções orientadas à tarefa, não agregações síncronas de diversos microsserviços a cada tela. Views planejadas: `ClinicalPatientView`, `TriageWorklistView`, `PatientCurrentVisitView`, `QueueDashboardView` e `PreArrivalEmergencyView`.

**Motivo:** CQRS permite otimizar leitura independentemente da escrita e reduz acoplamento/latência no ponto de cuidado.
