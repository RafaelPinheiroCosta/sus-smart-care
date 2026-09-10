# Prehospital Service

Responsavel pelo contexto pre-hospitalar do SUS Smart Care.

## Responsabilidades

- Cadastro e estado operacional das ambulancias.
- Cobertura operacional temporal das ambulancias.
- Pre-cadastro de atendimento antes da chegada.
- ETA de chegada.
- Sinalizacao de risco pre-hospitalar.
- Lifecycle EN_ROUTE -> ARRIVED ou CANCELLED.
- Publicacao de eventos via transactional outbox.

## Regra clinica

O nivel de risco informado pela ambulancia serve como sinal para antecipacao e preparacao da unidade.

Ele nao substitui a classificacao clinica realizada pelo servico de triagem.

## Ambulancia e cobertura

Uma ambulancia precisa:

1. existir na frota;
2. estar ACTIVE;
3. possuir cobertura operacional ativa;

para iniciar um novo atendimento pre-hospitalar.

A unidade de cobertura nao precisa ser a mesma unidade de destino do paciente.

## Endpoints

Base:

`/api/v1/pre-hospital`

Recursos principais:

- `/ambulances`
- `/ambulances/{ambulanceId}/operational-status`
- `/ambulances/{ambulanceId}/coverages`
- `/ambulances/{ambulanceId}/coverage`
- `/ambulance-coverages/{coverageId}/end`
- `/encounters`
- `/encounters/{id}/eta`
- `/encounters/{id}/risk`
- `/encounters/{id}/arrive`
- `/encounters/{id}/cancel`
