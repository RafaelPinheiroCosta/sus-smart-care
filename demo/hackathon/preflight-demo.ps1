param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$KeycloakUrl = "http://localhost:8180",
  [string]$PrometheusUrl = "http://localhost:9090",
  [string]$GrafanaUrl = "http://localhost:3000",
  [string]$TempoUrl = "http://localhost:3200",
  [string]$ComposeFile = "docker-compose.full.yml"
)

$ErrorActionPreference = "Continue"

function Check-Http(
  [string]$Name,
  [string]$Url,
  [int]$Attempts = 5,
  [int]$DelaySeconds = 2
) {
  $lastError = $null

  for ($attempt = 1; $attempt -le $Attempts; $attempt++) {

    try {

      $r = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri $Url `
        -TimeoutSec 5

      if ([int]$r.StatusCode -eq 200) {

        Write-Host (
          "[OK]   {0,-18} {1}" -f $Name,$Url
        ) -ForegroundColor Green

        return $true
      }

      $lastError = "HTTP $($r.StatusCode)"
    }
    catch {

      $lastError = $_.Exception.Message
    }

    if ($attempt -lt $Attempts) {

      Write-Host (
        "[WAIT] {0,-18} tentativa {1}/{2}; nova tentativa em {3}s" `
          -f $Name,$attempt,$Attempts,$DelaySeconds
      ) -ForegroundColor Yellow

      Start-Sleep -Seconds $DelaySeconds
    }
  }

  Write-Host (
    "[FAIL] {0,-18} apos {1} tentativas: {2}" `
      -f $Name,$Attempts,$lastError
  ) -ForegroundColor Red

  return $false
}
Write-Host ""
Write-Host "=== SUS SMART CARE - PRE-FLIGHT DA DEMONSTRACAO ===" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $ComposeFile) {
  Write-Host "--- Docker Compose ---" -ForegroundColor Cyan
  docker compose -f $ComposeFile ps
  Write-Host ""
} else {
  Write-Host "[WARN] $ComposeFile nao encontrado no diretorio atual." -ForegroundColor Yellow
}

$results = @()
$results += Check-Http "API Gateway" "$BaseUrl/actuator/health"
$results += Check-Http "Keycloak" "$KeycloakUrl/realms/sus-smart-care/.well-known/openid-configuration"
$results += Check-Http "Prometheus" "$PrometheusUrl/-/ready"
$results += Check-Http "Grafana" "$GrafanaUrl/api/health"
$results += Check-Http "Tempo" "$TempoUrl/ready"

Write-Host ""
Write-Host "--- Token demo.admin ---" -ForegroundColor Cyan
try {
  $t = Invoke-RestMethod `
    -Method Post `
    -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
      client_id = "sus-smart-care-postman"
      grant_type = "password"
      username = "demo.admin"
      password = "admin123"
    }

  if ($t.access_token) {
    Write-Host "[OK] token ADMIN obtido" -ForegroundColor Green
  }
} catch {
  Write-Host "[FAIL] token ADMIN: $($_.Exception.Message)" -ForegroundColor Red
  $results += $false
}

Write-Host ""
if ($results -contains $false) {
  Write-Host "PRE-FLIGHT COM FALHAS. Corrija antes de gravar." -ForegroundColor Red
  exit 1
}

Write-Host "PRE-FLIGHT OK." -ForegroundColor Green
