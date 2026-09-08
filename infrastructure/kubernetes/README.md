# Kubernetes

Manifests de referência para demonstrar scale-out, health probes e HPA. Antes de aplicar, substitua `REPLACE_ACR` pelo Azure Container Registry e mova credenciais de banco/Keycloak para `Secret`/Key Vault CSI. Os manifests não implantam bancos nem Kafka; em produção estes devem ser serviços gerenciados ou clusters dedicados.
