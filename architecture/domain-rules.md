# Regras e invariantes de domínio

1. **Equidade de entrada**: APP, responsável, totem, recepção e ambulância não alteram prioridade clínica.
2. **Identidade desacoplada**: Patient existe independentemente de UserAccount.
3. **Representação temporal**: vínculos de pai/tutor/cuidador têm vigência, permissões e auditoria.
4. **Histórico estável**: mudança de autonomia nunca cria um novo histórico clínico.
5. **Identidade provisória**: atendimento pode iniciar com dados incompletos; resolução posterior preserva o histórico.
6. **Merge auditável**: registros duplicados são mesclados por processo explícito, preservando IDs de origem.
7. **Human-in-the-loop**: IA pode sugerir risco/prioridade; decisão final é profissional e justificável.
8. **Telemetria desacoplada**: Triage recebe observações normalizadas, não protocolos proprietários de device.
9. **Presença multicanal**: ausência de smartphone não impede registrar saída/retorno/transição.
10. **Tolerância configurável**: saída temporária gera prazo esperado e grace period conforme política da unidade.
11. **Atraso operacional**: ultrapassar tolerância pode perder posição/ser re-enfileirado; classificação clínica não é rebaixada automaticamente.
12. **Privacidade de telão**: exibição pública usa identificador anonimizado/senha, nunca dado clínico sensível.
13. **Ambulância antecipa informação**: telemetria/ETA pode preparar recursos e influenciar previsão operacional, mas não cria “prioridade por meio de transporte”.
14. **Fallback clínico**: indisponibilidade de IA não bloqueia triagem; regras e decisão humana permanecem disponíveis.
15. **Rastreabilidade**: decisões críticas registram quem, quando, canal, contexto, versão de regras/modelo e eventual override.
