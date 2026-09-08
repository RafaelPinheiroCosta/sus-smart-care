# ADR-018 — IA como porta substituível e falha não crítica
**Decisão:** Triage depende de `RiskAssessmentPort`. O adapter HTTP usa Circuit Breaker + Retry; falha retorna fallback seguro e sempre exige decisão profissional. O provider `demo` não executa classificação clínica real e existe apenas para demonstrar a integração.
**Regra:** indisponibilidade ou baixa confiança da IA nunca impede triagem humana.
