param(
  [string]$SessionId = "",
  [string]$ComposeFile = "docker-compose.full.yml"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ComposeFile)) {
  throw "Nao encontrei $ComposeFile. Execute a partir da raiz do projeto."
}

if (-not $SessionId) {
  $stateFile = Join-Path $PSScriptRoot ".demo-state\mqtt-state.json"
  if (Test-Path $stateFile) {
    $state = Get-Content -Raw $stateFile | ConvertFrom-Json
    $SessionId = [string]$state.sessionId
  }
}

if (-not $SessionId) {
  throw "Informe -SessionId ou execute o robo MQTT com -SessionId."
}

$pattern = "telemetry:rolling:$SessionId:*"

Write-Host ""
Write-Host "=== REDIS - JANELA QUENTE DE TELEMETRIA ===" -ForegroundColor Cyan
Write-Host "Pattern: $pattern"
Write-Host ""

$keys = @(
  docker compose -f $ComposeFile exec -T redis `
    redis-cli --scan --pattern $pattern
) | ForEach-Object { $_.Trim() } | Where-Object { $_ }

if ($keys.Count -eq 0) {
  Write-Host "Nenhuma chave encontrada ainda. Execute o robo e tente novamente." -ForegroundColor Yellow
  exit 2
}

foreach ($key in $keys) {
  $count = (
    docker compose -f $ComposeFile exec -T redis redis-cli LLEN $key
  ).Trim()

  Write-Host $key -ForegroundColor Green
  Write-Host "  LLEN = $count"
  Write-Host "  3 amostras mais recentes:"

  docker compose -f $ComposeFile exec -T redis `
    redis-cli LRANGE $key 0 2 |
    ForEach-Object { Write-Host "    $_" }

  Write-Host ""
}

Write-Host "Padrao atual: rollingSize=120 e TTL=120 minutos." -ForegroundColor Cyan
