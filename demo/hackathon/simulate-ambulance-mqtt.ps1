param(
  [Parameter(Mandatory=$true)][string]$DeviceExternalId,
  [string]$SessionId = "",
  [int]$Samples = 30,
  [int]$IntervalMs = 500,
  [string]$ComposeFile = "docker-compose.full.yml",
  [string]$MqttUser = "device-demo",
  [string]$MqttPassword = "device-demo-secret"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ComposeFile)) {
  throw "Execute a partir da raiz do projeto ou informe -ComposeFile. Nao encontrei: $ComposeFile"
}

$topic = "sus/v1/devices/$DeviceExternalId/telemetry"
$stateDir = Join-Path $PSScriptRoot ".demo-state"
New-Item -ItemType Directory -Force -Path $stateDir | Out-Null
$stateFile = Join-Path $stateDir "mqtt-state.json"

function Publish-Payload([hashtable]$Payload) {
  $json = $Payload | ConvertTo-Json -Compress

  $json | docker compose -f $ComposeFile exec -T mosquitto `
    mosquitto_pub -h localhost -p 1883 `
    -u $MqttUser -P $MqttPassword `
    -q 1 -t $topic -s

  if ($LASTEXITCODE -ne 0) {
    throw "mosquitto_pub falhou com exit code $LASTEXITCODE"
  }

  @{
    deviceExternalId = $DeviceExternalId
    sessionId        = $SessionId
    topic            = $topic
    payload          = $Payload
    savedAt          = [DateTime]::UtcNow.ToString("o")
  } | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 $stateFile
}

$baseSequence = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Host ""
Write-Host "=== AMBULANCIA MQTT - TELEMETRIA CONTINUA ===" -ForegroundColor Cyan
Write-Host "Device : $DeviceExternalId"
Write-Host "Topic  : $topic"
if ($SessionId) { Write-Host "Session: $SessionId" }
Write-Host "Amostras normais: $Samples | intervalo: ${IntervalMs}ms"
Write-Host ""

for ($i=1; $i -le $Samples; $i++) {
  if ($i % 5 -eq 0) {
    $type = "SPO2"
    $value = Get-Random -Minimum 92 -Maximum 99
    $unit = "%"
  } else {
    $type = "HEART_RATE"
    $value = Get-Random -Minimum 96 -Maximum 126
    $unit = "bpm"
  }

  $payload = @{
    messageId  = [guid]::NewGuid().ToString()
    sequence   = $baseSequence + $i
    type       = $type
    value      = $value
    unit       = $unit
    measuredAt = [DateTime]::UtcNow.ToString("o")
  }

  Publish-Payload $payload
  Write-Host ("[{0:D2}] {1,-11} {2,5} {3,-3} seq={4}" -f $i,$type,$value,$unit,$payload.sequence)
  Start-Sleep -Milliseconds $IntervalMs
}

# Anomalia intencional. A policy atual considera SPO2 < 90 anomalo.
$anomaly = @{
  messageId  = [guid]::NewGuid().ToString()
  sequence   = $baseSequence + $Samples + 1
  type       = "SPO2"
  value      = 85
  unit       = "%"
  measuredAt = [DateTime]::UtcNow.ToString("o")
}

Publish-Payload $anomaly

Write-Host ""
Write-Host ("[ANOMALIA] SPO2 85% seq={0}" -f $anomaly.sequence) -ForegroundColor Yellow
Write-Host "Ultimo payload salvo em: $stateFile" -ForegroundColor Cyan
Write-Host "Para demonstrar idempotencia execute depois:"
Write-Host "  .\demo\hackathon\resend-last-mqtt.ps1"
