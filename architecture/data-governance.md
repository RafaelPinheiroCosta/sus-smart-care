# Governança, privacidade e dados sensíveis

O projeto manipula dados pessoais, de saúde, localização/presença e telemetria. Este documento registra **princípios técnicos do MVP**, sem alegar conformidade jurídica completa. Uma implantação real deve passar por avaliação institucional, jurídica, segurança da informação e governança clínica.

## Princípios

1. **Minimização:** cada bounded context recebe somente os dados necessários para sua responsabilidade.
2. **Paciente separado de autenticação:** histórico clínico referencia `patientId`; credenciais permanecem no Identity Provider.
3. **Representação auditável:** pai, tutor ou cuidador atua em nome do paciente sem se tornar dono do histórico.
4. **Identidade provisória:** atendimento pode iniciar sem identificação completa; resolução/merge preserva a trilha histórica.
5. **Sem dado clínico em telão público:** dashboard usa `displayCode`, posição/tempo aproximado e estado operacional.
6. **Logs sem payload clínico bruto:** `traceId`, `correlationId`, `visitId` e metadados técnicos são preferíveis a conteúdo de anamnese/biometria.
7. **Privilégio mínimo:** patient, staff, device e service clients devem possuir scopes distintos.
8. **Telemetria autenticada:** dispositivo/gateway precisa de identidade técnica antes de publicar dados.
9. **Criptografia:** TLS em trânsito e criptografia em repouso devem ser exigidas no ambiente real.
10. **Retenção:** prazos de dados clínicos, eventos, telemetria bruta e observabilidade devem ser definidos por política institucional; o MVP não inventa prazos legais.

## Dados por exposição

| Dado | API pública/telão | Paciente/representante | Profissional autorizado | Serviço interno |
|---|---|---|---|---|
| Display code | Sim | Sim | Sim | Sim |
| Nome/identidade | Não | Conforme vínculo | Sim | Quando necessário |
| Posição/tempo estimado | Agregado/anônimo | Sim | Sim | Sim |
| Anamnese | Não | Conforme regra | Sim | Triage/Query |
| Biometria | Não | Conforme produto futuro | Sim | Telemetry/Triage/Query |
| Recomendação IA | Não | Não no MVP | Sim, com versão/explicação | Triage/Query |
| Localização lógica | Não | Estado próprio | Sim quando necessário | Presence/Queue |

## Merge de identidade

O merge nunca apaga silenciosamente a origem. O registro antigo fica marcado como `MERGED` e aponta para o paciente canônico. Eventos históricos continuam correlacionáveis e as projeções são reassociadas ao identificador canônico.
