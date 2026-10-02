# SUS Smart Care — Kit de demonstração do Hackathon

Este diretório contém os artefatos usados para ensaiar e gravar o vídeo do MVP na mesma ordem do roteiro final.

Nenhum arquivo deste diretório altera código de produção. O objetivo é preparar e demonstrar cenários reais contra o stack `docker-compose.full.yml`.

## Arquivos

- `generate-postman-collection.ps1` — fonte da coleção Postman.
- `generate-postman-collection-safe.ps1` — gera e valida a coleção importável.
- `preflight-demo.ps1` — valida stack, endpoints básicos e autenticação antes do ensaio.
- `simulate-triage-vitals.ps1` — envia três sinais SPOT únicos para a triagem de Lucas.
- `listen-ambulance-acks.ps1` — escuta ACKs MQTT da ambulância.
- `simulate-ambulance-mqtt.ps1` — simula telemetria contínua e injeta uma anomalia de SPO2=85.
- `resend-last-mqtt.ps1` — reenvia a última mensagem para demonstrar idempotência (`DUPLICATE`).
- `show-redis-telemetry.ps1` — mostra a janela quente `telemetry:rolling:*` diretamente no Redis.

## 1. Atualizar o projeto

Na raiz do repositório:

```powershell
git pull origin master
```

## 2. Gerar a coleção Postman

```powershell
.\demo\hackathon\generate-postman-collection-safe.ps1
```

Arquivo gerado:

```text
demo/hackathon/SUS-Smart-Care-Hackathon-Demo-FINAL.postman_collection.json
```

O script faz duas validações antes de terminar:

1. sintaxe PowerShell do gerador;
2. parse do JSON e presença das seis pastas principais.

Importe esse JSON no Postman.

> Nos folders marcados como `RUNNER`, habilite **Keep variable values** para preservar IDs e tokens capturados como collection variables.

## 3. Subir e validar o stack

Use o procedimento normal do projeto para iniciar o full stack e depois rode:

```powershell
.\demo\hackathon\preflight-demo.ps1
```

O pre-flight deve mostrar `OK` para:

- API Gateway;
- Keycloak;
- Prometheus;
- Grafana;
- Tempo;
- token `demo.admin`.

Só inicie o ensaio quando o pre-flight terminar sem falhas.

---

## Regressão automática completa

Além da execução manual usada na gravação, a coleção pode ser executada integralmente pelo Collection Runner/Newman.

Nesse modo:

- o `PATIENT` é criado temporariamente no Keycloak;
- as projeções assíncronas usam polling;
- a telemetria da ambulância possui fallback HTTP automático;
- ambulância e cobertura são criadas para o próprio ensaio;
- o usuário temporário é removido ao final.

A regressão completa validada executa as seis jornadas sem depender de estado clínico de uma execução anterior.

---

# Sequência de teste
## 00 — Autenticação e preparação

Execute manualmente a pasta `00 - AUTENTICACAO E PREPARACAO` de cima para baixo.

A request `00.0` cria um novo `demoRunId`, evitando colisões de códigos e identificadores entre ensaios.

Os usuários operacionais fixos são autenticados normalmente. Para o papel `PATIENT`, a própria coleção cria uma conta efêmera no Keycloak para cada execução, atribui a role `PATIENT`, captura o `sub` real e obtém o token dessa conta.

Isso torna o cenário `PARENT -> SELF` repetível sem violar a regra de domínio que permite apenas um vínculo `SELF` ativo por conta.

Resultado final esperado:

- todos os logins `200`;
- criação do PATIENT efêmero `201`;
- atribuição da role `PATIENT` `204`;
- unidade principal `201`;
- `mainFacilityId`, `patientUserId` e tokens preenchidos nas collection variables.

A conta efêmera é removida automaticamente no final da pasta `05`.
---

# 01 — Lucas criança: jornada completa

Esta é a jornada principal e deve ser executada quase toda manualmente durante a gravação.

## Representação

Execute `01.01` até `01.07`.

Pontos para validar:

- Lucas possui um `patientId` próprio;
- Ana possui vínculo `PARENT`;
- `01.03` deve retornar `reason = ACTIVE_REPRESENTATION`;
- as operações do paciente usam o token da própria representante, não ADMIN.

## Falha de máquina de estados

Execute:

```text
01.08 FALHA ESPERADA - segundo check-in
```

Esperado:

```text
HTTP 409
```

O teste do Postman deve ficar verde e a resposta deve ser um `ProblemDetail` com `correlationId`.

## Triagem e IoT

Execute `01.09` até `01.14`.

Após `01.14`, copie das collection variables:

```text
lucasTriageSessionId
lucasTriageDeviceId
```

Na raiz do projeto rode:

```powershell
.\demo\hackathon\simulate-triage-vitals.ps1 `
  -SessionId "<lucasTriageSessionId>" `
  -DeviceId "<lucasTriageDeviceId>"
```

O script deve enviar:

- `HEART_RATE`;
- `SPO2`;
- `TEMPERATURE`.

Todos em modo SPOT, com `status = SPOT_PERSISTED`.

Se quiser testar sem PowerShell, existe a pasta `01.15 FALLBACK Postman - sinais vitais`. Não execute os dois durante a gravação.

Continue `01.16` a `01.21`.

A `Clinical View` do médico deve consolidar o mesmo `lucasPatientId` e os dados do atendimento.

> Clinical Query é eventualmente consistente. Se uma Clinical View imediatamente após um evento ainda retornar `404` ou estiver com um campo atrasado, aguarde cerca de um segundo e envie novamente. Isso é comportamento esperado do read model assíncrono; para a gravação, faça o ensaio antes para conhecer o tempo local do seu computador.

## Saída temporária

Execute `01.22` a `01.25`.

Validar:

- Lucas entra LOW;
- após a saída: `OUTSIDE_FACILITY` + `RETURN_REQUIRED`;
- histórico de notificações contém `RETURN_REQUIRED`;
- tentar chamar Lucas enquanto está fora retorna `409` e o teste fica verde.

## Carlos HIGH — adulto representado

Abra:

```text
01.30 MANUAL - entrada Carlos
```

Execute manualmente as requests desse folder para mostrar o adulto com `CAREGIVER`.

Depois rode no Collection Runner somente:

```text
01.30 RUNNER - Carlos ate fila HIGH
```

Com **Keep variable values** ligado.

Volte para a coleção e execute:

```text
01.32 FILA AGORA - Carlos > Lucas
```

O teste deve provar que HIGH está antes de LOW.

## Mariana MEDIUM

Faça o mesmo padrão:

1. execute `01.40 MANUAL - entrada Mariana`;
2. Runner em `01.40 RUNNER - Mariana ate fila MEDIUM`;
3. execute `01.42 FILA AGORA - HIGH > MEDIUM > LOW`.

## Paulo LOW

1. execute `01.50 MANUAL - entrada Paulo`;
2. Runner em `01.50 RUNNER - Paulo ate fila LOW`;
3. execute `01.52 FILA AGORA - Lucas LOW antes de Paulo LOW`.

Essa última request valida simultaneamente:

- Lucas continua fora;
- Lucas e Paulo têm LOW;
- Lucas permanece antes de Paulo porque entrou primeiro.

## Retorno e fila andando

Execute:

```text
01.60 Ana registra retorno
01.61 FILA AGORA - retorno preservado
```

Depois rode a pasta:

```text
01.62 RUNNER - chamar Carlos e Mariana
```

Volte e execute:

```text
01.63 FILA AGORA - Lucas 1 Paulo 2
```

Esperado: Lucas em posição 1 e Paulo atrás.

## Atendimento e alta

Execute `01.70` até `01.75`.

Validar:

- Lucas chamado;
- notification history contém `PATIENT_CALLED`;
- médico inicia atendimento;
- alta;
- Clinical View final possui o mesmo `patientId` e `journeyOutcome = DISCHARGED`.

Não apague as collection variables da infância. Elas serão reutilizadas na cena seguinte.

---

# 02 — Lucas futuro: PARENT para SELF

Execute manualmente:

```text
02.01 Revogar PARENT de Ana
02.02 FALHA ESPERADA - Ana sem vinculo
02.03 Vincular SELF a conta de Lucas
02.04 Provar SELF
```

Esperado:

- acesso de Ana após revogação: `403`;
- acesso da conta do próprio Lucas: `200` e `reason = SELF`;
- `lucasPatientId` não muda.

Depois use Collection Runner em:

```text
02.05 RUNNER - novo atendimento via SELF
```

Mantenha **Keep variable values** ligado.

Por fim execute manualmente:

```text
02.06 Clinical View infancia preservada
02.07 Clinical View atendimento futuro
```

As duas visitas devem possuir `visitId` diferentes e o mesmo `patientId`.

---

# 03 — Dona Rosa sem smartphone

Execute manualmente `03.01` a `03.14`.

A request:

```text
03.03 FALHA ESPERADA - sem smartphone + MOBILE
```

deve retornar `400`.

A seguinte corrige para:

```json
{
  "hasSmartphone": false,
  "queueCallMode": "DISPLAY_AND_VERBAL"
}
```

Dona Rosa continua pelo fluxo clínico normal até a fila com prioridade MEDIUM.

Depois rode no Runner:

```text
03.15 RUNNER - concorrentes da fila
```

Esse folder prepara um HIGH e um LOW na mesma unidade.

Volte para as requests manuais:

```text
03.16 Visao operacional - Dona Rosa normal
03.17 TELAO public-view sem PII
03.18 Totem - estimativa publica
```

`03.16` deve provar:

```text
HIGH > Dona Rosa MEDIUM > LOW
```

`03.17` deve provar que o `displayCode` de Dona Rosa aparece no telão sem `patientId`, `patientName` ou prioridade clínica.

`03.19` e `03.20` são opcionais para demonstrar chamada/notificação assistida.

---

# 04 — Ambulância + MQTT + Redis

A preparação agora é autocontida: a coleção cria a unidade destino, cadastra uma ambulância operacional, atribui sua cobertura e só então abre o encounter pré-hospitalar.

Para a demonstração manual, execute em ordem:

```text
04.01  Criar unidade destino
04.01A Cadastrar ambulancia do ensaio
04.01B Atribuir cobertura da ambulancia
04.02  Patient provisorio
04.03  PreVisit AMBULANCE
04.04  Encounter pre-hospitalar
04.05  Registrar monitor embarcado
04.06  Posicionar na ambulancia
04.07  Abrir sessao CONTINUOUS
04.08  Associar monitor a sessao
```

Após `04.08`, copie das collection variables:

```text
ambulanceDeviceExternalId
ambulanceTelemetrySessionId
```

## Terminal 1 — ACK

```powershell
.\demo\hackathon\listen-ambulance-acks.ps1 `
  -DeviceExternalId "<ambulanceDeviceExternalId>"
```

Deixe esse terminal aberto.

## Terminal 2 — robô da ambulância

```powershell
.\demo\hackathon\simulate-ambulance-mqtt.ps1 `
  -DeviceExternalId "<ambulanceDeviceExternalId>" `
  -SessionId "<ambulanceTelemetrySessionId>"
```

O robô envia 30 amostras normais e, ao final, uma anomalia intencional:

```text
SPO2 = 85%
```

No Terminal 1 devem aparecer ACKs `ACCEPTED`.

> Durante a gravação com MQTT, não é necessário executar manualmente `04.08A` e `04.08B`. Essas requests existem como fallback automático para o Collection Runner/Newman: enviam dez amostras `HEART_RATE` e uma amostra `SPO2=85` via HTTP.

## Terminal 3 — Redis

Enquanto ou logo após o robô estiver executando:

```powershell
.\demo\hackathon\show-redis-telemetry.ps1 `
  -SessionId "<ambulanceTelemetrySessionId>"
```

O script deve mostrar chaves no formato:

```text
telemetry:rolling:<sessionId>:HEART_RATE
telemetry:rolling:<sessionId>:SPO2
```

junto com `LLEN` e amostras recentes.

Volte ao Postman e execute:

```text
04.09 Poll agregados apos telemetria
04.10 Poll anomalias apos telemetria
```

Essas requests possuem polling para respeitar a consistência eventual do processamento.

Esperado:

- agregado `HEART_RATE` com pelo menos 10 amostras;
- anomalia `SPO2=85` persistida.

Quando a coleção completa é executada pelo Runner/Newman, `04.08A` e `04.08B` produzem automaticamente os dados necessários.

## Idempotência MQTT

Com o listener de ACK ainda aberto:

```powershell
.\demo\hackathon\resend-last-mqtt.ps1
```

Esperado no Terminal 1:

```text
status = DUPLICATE
```

A mesma mensagem não deve incrementar novamente o processamento clínico.

## Pré-chegada e continuidade

Continue:

```text
04.11 Risco pre-hospitalar HIGH
04.12 MEDICO - Clinical View ANTES da chegada
04.13 Marcar chegada encounter
04.14 Check-in MESMA Visit pelo OPERATOR
04.15 Poll triagem mesma Visit
```

Ponto principal: o `ambulanceVisitId` é criado antes da chegada e continua sendo usado no check-in hospitalar.

---

# 05 — Observabilidade e segurança

Execute:

```text
05.01  Prometheus targets
05.02  DOCTOR Clinical View -> 200
05.03  PATIENT Clinical View -> 403
05.03A Renovar KEYCLOAK ADMIN para cleanup
05.04  CLEANUP - remover PATIENT efemero
```

As duas últimas requests encerram o estado temporário criado para a regressão e permitem repetir a coleção sem reutilizar a identidade `SELF` da execução anterior.

Para a gravação, abra também no navegador:

```text
Prometheus: http://localhost:9090/targets
Grafana:    http://localhost:3000
Tempo:      datasource Tempo dentro do Grafana
```

A pasta Postman valida os endpoints; a interface do Grafana/Tempo será usada apenas para a parte visual do vídeo.

---

# Recomeçar um ensaio

Não reaproveite os IDs de uma execução anterior.

Comece sempre por:

```text
00.0 RESET DEMO RUN + Gateway Health
```

e execute novamente os logins e a criação das unidades/pacientes necessárias.

Os códigos de demonstração incluem `demoRunId`, e o usuário `PATIENT` usado no cenário `SELF` também é criado por execução e removido no final. Por isso a coleção completa pode ser executada repetidamente sem reutilizar IDs clínicos nem o vínculo `SELF` de um ensaio anterior.

# Falhas esperadas do roteiro

| Cena | Ação | Resultado esperado |
|---|---|---|
| Lucas criança | segundo check-in | 409 |
| Lucas criança | chamar enquanto fora | 409 |
| Lucas futuro | Ana acessa após revogação | 403 |
| Dona Rosa | sem smartphone + MOBILE | 400 |
| Ambulância | repetir messageId/sequence MQTT | ACK `DUPLICATE` |
| Segurança | PATIENT acessa Clinical View profissional | 403 |

Essas respostas são parte da demonstração. Um teste verde do Postman significa que a falha esperada foi corretamente tratada.
