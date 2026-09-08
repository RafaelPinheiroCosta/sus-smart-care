# Roteiros executáveis de demonstração — v0.4

Com a full stack em execução:

- `scripts/e2e-demo.ps1`: paciente digital, biometria, IA boundary, decisão humana, fila e View Data.
- `scripts/e2e-no-smartphone.ps1`: recepção, telão anonimizado, saída/retorno e tolerância sem alterar prioridade clínica.
- `scripts/e2e-ambulance.ps1`: identidade provisória, ambulância, telemetria pré-hospitalar, ETA/risco e preparação antes da chegada.
- `scripts/e2e-representation.ps1`: paciente inicialmente representado e posteriormente autônomo sem trocar `patientId`.
- `scripts/security-smoke.ps1`: smoke test de autorização por papel e endpoint público.

Os scripts são evidências úteis para o vídeo do MVP porque demonstram regras de negócio e arquitetura sem exigir front-end, exatamente o formato permitido pelo Hackathon.
