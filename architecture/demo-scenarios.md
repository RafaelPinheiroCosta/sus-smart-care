# Cenários de demonstração
## 1. Paciente com celular
Patient Registry → PreVisit → pré-anamnese → check-in → triagem → decisão profissional → fila → ClinicalPatientView.
## 2. Paciente sem smartphone
Defina preferência `hasSmartphone=false`; Presence registra saída/retorno por totem/recepção; Notification usa DISPLAY + STAFF_ASSISTED; atraso além da tolerância afeta apenas posição operacional.
## 3. Ambulância
Crie visita/identidade provisória se necessário → PreHospitalEncounter com `visitId` → TelemetrySession → dispositivos enviam observações → Triage mantém projeção biométrica → alerta pré-chegada → STAFF_CONSOLE.
## 4. Resiliência da IA
Configure `AI_PROVIDER=http` com provider indisponível. Circuit Breaker/Retry falham para fallback, e o profissional continua podendo confirmar prioridade.
## 5. CQRS e idempotência
Republique o mesmo envelope Kafka: `processed_events` evita dupla aplicação da projeção. Consulte Clinical Query e observe Redis/PostgreSQL.
