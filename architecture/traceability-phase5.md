# Rastreabilidade — conteúdos da Fase 5 x decisões do projeto

Este documento evita o uso de tecnologias apenas como demonstração de repertório. Cada item da Fase 5 é relacionado a um problema real do SUS Smart Care e recebe uma decisão explícita.

| Conteúdo | Aplicação no projeto | Decisão |
|---|---|---|
| Event Storming | Descoberta da jornada, comandos, eventos, agregados e bounded contexts | **Usado como origem da modelagem** |
| DDD / bounded contexts | Limites entre Patient Registry, Journey, Triage, Telemetry, Queue, Presence, Pre-Hospital e Notification | **Usado** |
| Microsserviços | Capacidades com motivos de mudança e perfis de carga diferentes | **Usado seletivamente** |
| Spring Boot | Serviços de domínio e read side | **Principal framework Java** |
| Spring Cloud Gateway | Entrada única, autenticação e roteamento | **Usado** |
| Eureka | Útil em ambiente Spring Cloud tradicional, mas redundante no Kubernetes | **Não previsto para produção** |
| Spring Cloud Config | Configuração centralizada é válida, mas no Kubernetes será coberta por ConfigMaps/Secrets e configuração externa | **Não obrigatório no MVP** |
| REST / OpenAPI | Comandos e consultas que exigem interação síncrona | **API-first** |
| Kafka | Backbone para eventos de integração, telemetria, projeções e notificações | **Usado** |
| RabbitMQ | Excelente para work queues, mas o MVP já possui Kafka e não há razão suficiente para dois brokers | **Deliberadamente não usado** |
| CQRS | Queue e Clinical Query têm padrões de leitura diferentes dos comandos | **Usado seletivamente** |
| View Data | ClinicalPatientView, dashboard público e views de fila | **Usado** |
| Event Sourcing | Visit e Triage exigem rastreabilidade/replay de mudanças relevantes | **Usado seletivamente** |
| EventStoreDB | Especializado para Event Sourcing; adapter previsto | **Evolução**; MVP usa stream append-only PostgreSQL |
| Redis | Cache/read model de baixa latência | **Usado no Clinical Query** |
| Resilience4j | Falha da IA não pode impedir triagem humana | **Circuit Breaker + Retry + fallback** |
| Escala horizontal | Telemetry, Queue e Query podem escalar independentemente | **Prevista em Kubernetes/HPA** |
| Alta disponibilidade | Eliminar SPOFs nos componentes críticos em produção | **Arquitetura preparada; infraestrutura completa é evolução** |
| Observabilidade | Diagnóstico de uma jornada distribuída | **Logs correlacionados + métricas + traces** |
| OpenTelemetry | Instrumentação neutra de tracing | **Usado** |
| Prometheus/Grafana/Tempo | Métricas, dashboards e traces locais | **Usado** |
| API Governance | Versionamento, contratos, autenticação, ProblemDetail e políticas de exposição | **Usado** |
| OAuth2/OIDC/JWT | Paciente, equipe e integrações com níveis distintos de acesso | **Keycloak no ambiente local** |
| Docker | Reprodutibilidade do ambiente | **Usado** |
| Kubernetes | Scale-out, health probes, HPA e service discovery | **Manifests preparados** |
| IaC | Repetibilidade da infraestrutura Azure | **Bicep preparado/evolutivo** |
| CI/CD | Compilação, testes, contratos e dependency check | **GitHub Actions** |
| Testes de carga | Consultas de fila/read model são candidatas a picos | **k6 preparado** |
| SRE | SLO, error budget, capacidade e redução de toil | **Plano não funcional documentado** |

## Tecnologias deliberadamente fora do MVP

- **Sharding:** não existe evidência de volume que justifique a complexidade agora.
- **Service Mesh:** Kubernetes + Resilience4j + OpenTelemetry atendem o objetivo do MVP.
- **Multi-region ativo/ativo:** custo e complexidade não são proporcionais ao hackathon; deve ser uma decisão de produção.
- **IA clínica real:** exige validação clínica, governança, dados adequados e avaliação de risco. `AI_PROVIDER=demo` apenas comprova o boundary arquitetural.
- **Integração real com equipamentos/SAMU:** o backend foi desenhado por adapters e contratos para receber essas integrações sem acoplar o domínio ao fabricante.
