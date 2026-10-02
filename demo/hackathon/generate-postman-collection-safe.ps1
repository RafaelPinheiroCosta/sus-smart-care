param([string]$OutputPath = "")

$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "generate-postman-collection.ps1"
if (-not (Test-Path $sourcePath)) {
  throw "Fonte da colecao nao encontrada: $sourcePath"
}
if (-not $OutputPath) {
  $OutputPath = Join-Path $PSScriptRoot "SUS-Smart-Care-Hackathon-Demo-FINAL.postman_collection.json"
}

# O arquivo-fonte foi escrito de modo conservador para sobreviver ao transporte
# pelo GitHub Contents API. Aqui normalizamos aspas dos JSON bodies e scripts JS.
$source = Get-Content -Raw $sourcePath
$source = $source.Replace('\"','"')

# Simplifica a unica linha que precisa imprimir contexto MQTT no Postman Console.
$replacement = @'
$am+=Request '04.08 Associar monitor a sessao' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions/{{ambulanceTelemetrySessionId}}/assignments' 'ambulanceToken' '{"deviceId":"{{ambulanceDeviceId}}"}' (Capture 'ambulanceAssignmentId' 201 'console.log("ambulanceDeviceExternalId",pm.collectionVariables.get("ambulanceDeviceExternalId"));console.log("ambulanceTelemetrySessionId",pm.collectionVariables.get("ambulanceTelemetrySessionId"));')
'@
$source = [regex]::Replace(
  $source,
  '(?m)^\$am\+=Request ''04\.08 Associar monitor a sessao''.*$',
  $replacement.TrimEnd()
)

$temp = Join-Path $env:TEMP ("ssc-postman-" + [guid]::NewGuid().ToString("N") + ".ps1")
try {
  Set-Content -Path $temp -Value $source -Encoding UTF8
  & $temp -OutputPath $OutputPath

  # Segunda validacao: JSON + estrutura minima.
  $c = Get-Content -Raw $OutputPath | ConvertFrom-Json
  if (-not $c.info -or -not $c.item -or $c.item.Count -lt 6) {
    throw "Colecao gerada, mas estrutura minima nao foi encontrada."
  }

  Write-Host ""
  Write-Host "OK - colecao pronta para importar no Postman:" -ForegroundColor Green
  Write-Host "  $OutputPath"
}
finally {
  Remove-Item $temp -ErrorAction SilentlyContinue
}
