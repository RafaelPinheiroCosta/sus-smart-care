# Modelo de autorização — v0.4

A autenticação é delegada ao Keycloak (OAuth2/OIDC/JWT). Os serviços convertem `realm_access.roles` em authorities `ROLE_*` e aplicam autorização coarse-grained no boundary HTTP. Fine-grained ownership (por exemplo, garantir que um `PATIENT` só opere o próprio `Patient`) é explicitamente uma evolução, porque exige vínculo seguro entre subject do token e `Patient/Representation`.

| Papel | Uso principal |
|---|---|
| `PATIENT` | pré-atendimento, anamnese, presença e consulta da própria jornada |
| `REPRESENTATIVE` | agir em nome de dependente quando vínculo permitir |
| `OPERATOR` | recepção/totem assistido e fluxo operacional |
| `TRIAGE_NURSE` | triagem e fila clínica |
| `DOCTOR` | View Data clínica e atendimento |
| `AMBULANCE_TEAM` | pré-hospitalar e telemetria autorizada |
| `DEVICE` | client-credentials para ingestão de dispositivo/gateway |
| `ADMIN` | administração/demonstração do MVP |

## Regra crítica
Possuir conta, smartphone ou papel de usuário **nunca concede prioridade clínica**. Autorização de sistema e priorização clínica são dimensões diferentes.
