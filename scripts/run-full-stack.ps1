param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$Root = Resolve-Path "$PSScriptRoot\.."
$Compose = "docker-compose.full.yml"

Push-Location $Root

try {

    function Wait-Http {
        param(
            [string]$Name,
            [string]$Url,
            [int]$Attempts = 60
        )

        for ($i = 1; $i -le $Attempts; $i++) {

            try {
                $response = Invoke-WebRequest `
                    -Uri $Url `
                    -UseBasicParsing `
                    -TimeoutSec 5

                if ($response.StatusCode -eq 200) {
                    Write-Host "[UP] $Name" -ForegroundColor Green
                    return
                }
            }
            catch {
            }

            Write-Host "[WAIT] $Name ($i/$Attempts)" -ForegroundColor Yellow
            Start-Sleep -Seconds 5
        }

        throw "$Name nao ficou pronto: $Url"
    }

    function Wait-Actuator {
        param(
            [string]$Name,
            [int]$Port
        )

        Wait-Http `
            -Name $Name `
            -Url "http://localhost:$Port/actuator/health" `
            -Attempts 60
    }

    if (-not $SkipBuild) {

        Write-Host "`n=== MAVEN ===" -ForegroundColor Cyan
        & "$PSScriptRoot\build-all.ps1"

        Write-Host "`n=== DOCKER BUILD ===" -ForegroundColor Cyan
        docker compose -f $Compose build
    }

    Write-Host "`n=== INFRA ESSENCIAL ===" -ForegroundColor Cyan

    docker compose -f $Compose up -d `
        postgres `
        redis `
        kafka `
        mosquitto `
        eventstore

    for ($i = 1; $i -le 60; $i++) {

        docker compose -f $Compose exec -T postgres `
            pg_isready -U sus *> $null

        if ($LASTEXITCODE -eq 0) {
            Write-Host "[UP] PostgreSQL" -ForegroundColor Green
            break
        }

        if ($i -eq 60) {
            throw "PostgreSQL nao ficou pronto."
        }

        Start-Sleep -Seconds 2
    }

    Write-Host "`n=== KEYCLOAK ===" -ForegroundColor Cyan

    docker compose -f $Compose up -d keycloak

    Wait-Http `
        -Name "Keycloak" `
        -Url "http://localhost:8180/realms/sus-smart-care/.well-known/openid-configuration" `
        -Attempts 120

    Write-Host "`n=== CORE ===" -ForegroundColor Cyan

    docker compose -f $Compose up -d `
        patient-registry-service `
        identity-access-service `
        facility-service `
        patient-journey-service

    Wait-Actuator "Patient Registry" 8081
    Wait-Actuator "Patient Journey" 8082
    Wait-Actuator "Identity Access" 8089
    Wait-Actuator "Facility" 8091

    Write-Host "`n=== CLINICA / FILA / IOT ===" -ForegroundColor Cyan

    docker compose -f $Compose up -d `
        triage-service `
        queue-service `
        telemetry-service `
        presence-service

    Wait-Actuator "Triage" 8083
    Wait-Actuator "Queue" 8084
    Wait-Actuator "Telemetry" 8085
    Wait-Actuator "Presence" 8086

    Write-Host "`n=== RESTANTE ===" -ForegroundColor Cyan

    docker compose -f $Compose up -d `
        prehospital-service `
        notification-service `
        clinical-query-service

    Wait-Actuator "Prehospital" 8087
    Wait-Actuator "Notification" 8088
    Wait-Actuator "Clinical Query" 8090

    Write-Host "`n=== GATEWAY ===" -ForegroundColor Cyan

    docker compose -f $Compose up -d api-gateway

    Wait-Actuator "API Gateway" 8080

    Write-Host "`n=== OBSERVABILIDADE ===" -ForegroundColor Cyan

    docker compose -f $Compose up -d `
        tempo `
        prometheus `
        grafana

    Write-Host "`n==============================================" -ForegroundColor Green
    Write-Host "FULL STACK SUS SMART CARE INICIADA" -ForegroundColor Green
    Write-Host "==============================================" -ForegroundColor Green

    Write-Host "Gateway:    http://localhost:8080"
    Write-Host "Keycloak:   http://localhost:8180"
    Write-Host "Grafana:    http://localhost:3000"
    Write-Host "Prometheus: http://localhost:9090"
    Write-Host "Tempo:      http://localhost:3200"

}
finally {
    Pop-Location
}