param(
  [Parameter(Mandatory=$true)][string]$SessionId,
  [Parameter(Mandatory=$true)][string]$DeviceId,
  [string]$BaseUrl = "http://localhost:8080",
  [string]$KeycloakUrl = "http://localhost:8180"
)

$ErrorActionPreference = "Stop"

function Get-DeviceToken {
  (Invoke-RestMethod `
    -Method Post `
    -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
      client_id     = "device-client"
      client_secret = "device-secret"
      grant_type    = "client_credentials"
    }).access_token
}

function Send-Vital([string]$Type,[double]$Value,[string]$Unit) {
  $headers = @{
    Authorization      = "Bearer $script:Token"
    "X-Correlation-Id" = [guid]::NewGuid().ToString()
  }

  $body = @{
    deviceId   = $DeviceId
    type       = $Type
    value      = $Value
    unit       = $Unit
    measuredAt = [DateTime]::UtcNow.ToString("o")
  } | ConvertTo-Json -Compress

  $result = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/v1/telemetry/sessions/$SessionId/observations" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $body

  Write-Host ("{0,-14} {1,6} {2,-4} -> {3}" -f $Type,$Value,$Unit,$result.status) -ForegroundColor Green
}

$script:Token = Get-DeviceToken

$hr   = Get-Random -Minimum 88 -Maximum 105
$spo2 = Get-Random -Minimum 96 -Maximum 100
$temp = [math]::Round((Get-Random -Minimum 365 -Maximum 379) / 10.0, 1)

Write-Host ""
Write-Host "=== TRIAGEM IOT - SINAIS UNICOS ===" -ForegroundColor Cyan
Write-Host "Session: $SessionId"
Write-Host "Device : $DeviceId"
Write-Host ""

Send-Vital "HEART_RATE"  $hr   "bpm"
Send-Vital "SPO2"        $spo2 "%"
Send-Vital "TEMPERATURE" $temp "C"

Write-Host ""
Write-Host "OK - tres sinais SPOT enviados e persistidos." -ForegroundColor Cyan
