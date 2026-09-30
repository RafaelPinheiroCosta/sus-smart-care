# Segurança, identidade e dados

## 1. Autenticação e autorização

O ambiente local usa Keycloak com OAuth2/OIDC/JWT. Os serviços funcionam como Resource Servers e convertem roles do realm em authorities da aplicação.

Papéis principais:

- `PATIENT`
- `REPRESENTATIVE`
- `OPERATOR`
- `TRIAGE_NURSE`
- `DOCTOR`
- `AMBULANCE_TEAM`
- `DEVICE`
- `ADMIN`

A autorização possui dois níveis: role no boundary HTTP e validação de ownership/vínculo nos recursos que exigem controle fino.

## 2. Patient separado da conta

`Patient` é a identidade clínica canônica. `UserAccount` representa autenticação. Essa separação permite que:

- o paciente exista sem conta;
- uma criança seja operada por responsável e depois ganhe autonomia;
- um idoso tenha cuidador temporário;
- vínculos sejam revogados sem perder o histórico;
- atendimento comece com identidade provisória e seja resolvido posteriormente.

O histórico permanece ligado ao mesmo `patientId` canônico.

## 3. Controles fine-grained implementados

- Patient Registry avalia acesso SELF e representação ativa.
- Queue valida o ator em relação à visita antes de expor/alterar recursos protegidos.
- Presence valida ownership da visita e coerência do `patientId`.
- Notification protege histórico pelo acesso ao paciente; o perfil de comunicação tem Patient Registry como fonte de verdade.
- Triage deriva a identidade do profissional que confirma a prioridade a partir do `sub` do JWT.

## 4. Dados sensíveis e exposição

Cada contexto deve receber apenas os dados necessários para sua responsabilidade. Telões e consultas públicas usam identificador anonimizado (`displayCode`) e informações operacionais; dados clínicos não são expostos publicamente.

Logs devem privilegiar identificadores técnicos, `correlationId`, `traceId`, `visitId` e metadados, evitando payload clínico bruto.

## 5. Telemetria e dispositivos

O payload MQTT não contém identificadores clínicos do paciente. A associação entre dispositivo e paciente/sessão é resolvida dentro da plataforma por placement/assignment temporal.

As credenciais MQTT e os usuários existentes no realm são apenas para desenvolvimento local. Implantação real exige TLS, ACL por dispositivo/gateway, rotação de segredo e integração com cofre de segredos.

## 6. Privacidade e produção

Este MVP registra princípios técnicos; ele não declara conformidade jurídica completa nem validação clínica. Produção exige avaliação institucional, LGPD/governança de saúde, política de retenção, criptografia em repouso/trânsito, gestão de segredos, trilha de auditoria e revisão dos perfis de acesso.
