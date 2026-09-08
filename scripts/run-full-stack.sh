#!/usr/bin/env bash
set -euo pipefail
"$(dirname "$0")/build-all.sh"
docker compose -f docker-compose.full.yml up -d --build
echo 'Stack iniciada: gateway http://localhost:8080 | keycloak http://localhost:8180 | grafana http://localhost:3000'
