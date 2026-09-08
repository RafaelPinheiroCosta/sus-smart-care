# ADR-014 — Equivalência para paciente sem smartphone

**Decisão:** ausência de smartphone é uma preferência/capacidade de comunicação, nunca uma condição de domínio clínico. Telão anonimizado, totem e chamada verbal/assistida são canais equivalentes. Saída e retorno podem ser registrados por staff/totem e obedecem à mesma `ReturnPolicy`.

**Dashboard presencial:** Queue expõe `public-view` anonimizada por `displayCode`; nenhum nome, CPF/CNS ou dado clínico é mostrado no telão.
