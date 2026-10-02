param(
  [string]$ComposeFile = "docker-compose.full.yml",
  [string]$MqttUser = "device-demo",
  [string]$MqttPassword = "device-demo-secret"
)

$ErrorActionPreference = "Stop"

$stateFile = Join-Path $PSScriptRoot ".demo-state\mqtt-state.json"

if (-not (Test-Path $stateFile)) {
  throw "Estado MQTT nao encontrado. Execute primeiro simulate-ambulance-mqtt.ps1"
}
if (-not (Test-Path $ComposeFile)) {
  throw "Nao encontrei $ComposeFile. Execute a partir da raiz do projeto."
}

$state = Get-Content -Raw $stateFile | ConvertFrom-Json
$topic = [string]$state.topic
$json = $state.payload | ConvertTo-Json -Compress

Write-Host ""
Write-Host "=== REENVIO INTENCIONAL DA ULTIMA MENSAGEM ===" -ForegroundColor Yellow
Write-Host "messageId: $($state.payload.messageId)"
Write-Host "sequence : $($state.payload.sequence)"
Write-Host "Esperado no ACK: DUPLICATE"
Write-Host ""

$json | docker compose -f $ComposeFile exec -T mosquitto `
  mosquitto_pub -h localhost -p 1883 `
  -u $MqttUser -P $MqttPassword `
  -q 1 -t $topic -s

if ($LASTEXITCODE -ne 0) {
  throw "mosquitto_pub falhou com exit code $LASTEXITCODE"
}

Write-Host "Mensagem reenviada. Observe o terminal de ACK." -ForegroundColor Cyan
