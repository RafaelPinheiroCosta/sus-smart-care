$ErrorActionPreference = "Stop"
& "$PSScriptRoot/build-all.ps1"
docker compose -f docker-compose.full.yml up -d --build
Write-Host "Stack iniciada. Gateway: http://localhost:8080 | Keycloak: http://localhost:8180 | Grafana: http://localhost:3000"
