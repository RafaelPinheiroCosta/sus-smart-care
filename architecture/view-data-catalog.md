# View Data / Read Models

## ClinicalPatientView
**Consumidor:** médico/equipe assistencial.  
**Objetivo:** visão consolidada sem joins distribuídos em tempo real.

Campos previstos:
- patientId, visitId, identidade essencial;
- motivo da procura;
- resumo de pré-anamnese;
- triagem validada;
- últimas biometrias relevantes e tendências;
- prioridade clínica final;
- recomendação IA + versão + explicação (quando disponível);
- presença atual;
- etapa atual da jornada;
- alertas e pendências.

## PatientCurrentVisitView
**Consumidor:** app/totem.  
- posição aproximada;
- faixa de tempo estimado;
- próxima etapa;
- necessidade de retorno;
- prazo e tolerância;
- canal de chamada.

## QueueDashboardView
**Consumidor:** telão/recepção/gestão.  
- identificador público anonimizado;
- posição/faixa estimada;
- status de chamada;
- sala/destino quando chamado;
- nunca expõe diagnóstico ou dados clínicos sensíveis.

## TriageWorklistView
**Consumidor:** triagem.  
- pacientes aguardando;
- pré-anamnese disponível;
- presença;
- origem/canal;
- dados pré-hospitalares relevantes.

## PreArrivalEmergencyView
**Consumidor:** hospital antes da chegada de ambulância.  
- ETA;
- status pré-hospitalar;
- alertas derivados;
- telemetria resumida/tendência;
- preparação requerida.

## OperationalUnitView
**Consumidor:** coordenação.  
- pacientes por etapa/prioridade;
- tempos médios;
- throughput;
- abandonos/atrasos;
- chegadas críticas previstas;
- indicadores de capacidade.

## Persistência inicial
- PostgreSQL para projeções clínicas/operacionais persistentes.
- Redis para views quentes de fila/presença e cache.
- Evolução pode extrair Query Services independentes se carga justificar.
