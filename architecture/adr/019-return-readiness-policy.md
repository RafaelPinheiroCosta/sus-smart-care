# ADR-019 — Prioridade clínica separada de disponibilidade operacional
Paciente fora da unidade preserva sua `clinicalPriority`. Quando se aproxima da chamada, a fila gera `RETURN_REQUIRED`, aplica janela e tolerância configuráveis e, se a tolerância expirar, marca `MISSED_CALL`. Ao retornar tarde, volta à mesma faixa clínica, porém com nova antiguidade operacional.
