# ADR-012 — Gateway e identidade

**Decisão:** Spring Cloud Gateway como entrada única; autenticação/autorizações via OAuth2/OIDC/JWT com Keycloak no ambiente local. `Patient` permanece independente de `UserAccount`.

**Consequência:** segurança centralizada no edge e validada também nos resource servers; evita que cada serviço gerencie senhas.
