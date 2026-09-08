# Requisitos não funcionais e SRE

Os valores abaixo são **metas arquiteturais de projeto**, não SLAs oficiais do SUS. Precisam ser recalibrados com dados reais de uma unidade de saúde.

## Classes de criticidade

### Classe A — fluxo clínico essencial
Patient Registry, Patient Journey, Triage e Queue. A indisponibilidade prolongada compromete o fluxo assistencial. O sistema deve possuir fallback operacional/manual documentado.

### Classe B — apoio operacional em tempo quase real
Presence, Telemetry, Pre-Hospital e Clinical Query. A perda permanente de eventos não é aceitável; atrasos temporários podem ser absorvidos por buffering/reprocessamento.

### Classe C — comunicação complementar
Notification. Falhas devem gerar retry/DLT, mas não podem cancelar triagem, atendimento ou decisão clínica.

## SLOs iniciais para ambiente de referência

| Indicador | Meta inicial | Observação |
|---|---:|---|
| Disponibilidade API de fila/query | 99,9% | meta de projeto, a validar |
| p95 consulta de posição/View Data | < 500 ms | read side/cache quente |
| p95 comando administrativo REST | < 1 s | exclui dependências externas |
| atraso p95 de projeção Kafka → View Data | < 5 s | consistência eventual explícita |
| perda de evento confirmado no outbox | 0 | retry + persistência local |
| recuperação automática de consumer transitório | < 2 min | retry/DLT e health checks |

## Capacidade e escala

- Serviços devem ser stateless sempre que possível.
- Telemetry escala por taxa de ingestão e número de partições Kafka.
- Clinical Query e Queue escalam por leitura; Redis reduz pressão na persistência.
- HPA usa CPU/memória inicialmente; métricas de negócio/lag são evolução recomendada.
- Testes k6 devem medir pelo menos consultas públicas de fila e Clinical Query antes da entrega final.

## Disponibilidade e recuperação

- Produção deve usar bancos gerenciados com backup e alta disponibilidade quando disponível.
- Kafka/event streaming deve possuir replicação adequada ao ambiente.
- RPO/RTO reais dependem da unidade/provedor e não são inventados pelo MVP.
- Outbox e consumidores idempotentes evitam perda/duplicação lógica em falhas parciais.
- DLT não é destino final: deve possuir runbook para inspeção, correção e replay.

## Degradação graciosa

- IA indisponível → classificação continua por profissional/regras aprovadas.
- Notification indisponível → atendimento continua; evento permanece recuperável.
- Redis indisponível → Clinical Query tenta persistência da projeção.
- Aplicativo indisponível/sem smartphone → telão/totem/chamada assistida e registro presencial de saída/retorno.
