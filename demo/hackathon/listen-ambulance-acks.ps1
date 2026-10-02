param(
  [Parameter(Mandatory=$true)][string]$DeviceExternalId,
  [string]$ComposeFile = "docker-compose.full.yml",
  [string]$MqttUser = "device-demo",
  [string]$MqttPassword = "device-demo-secret"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ComposeFile)) {
  throw "Nao encontrei $ComposeFile. Execute a partir da raiz do projeto."
}

$topic = "sus/v1/devices/$DeviceExternalId/ack"

Write-Host ""
Write-Host "=== MQTT ACK LISTENER ===" -ForegroundColor Cyan
Write-Host "Topic: $topic"
Write-Host "Esperados: ACCEPTED, DUPLICATE ou REJECTED"
Write-Host "Ctrl+C para encerrar."
Write-Host ""

docker compose -f $ComposeFile exec -T mosquitto `
  mosquitto_sub -h localhost -p 1883 `
  -u $MqttUser -P $MqttPassword `
  -q 1 -t $topic -v
