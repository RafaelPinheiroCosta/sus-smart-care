param()

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== VALIDACAO ESTATICA DO KIT DE DEMONSTRACAO ===" -ForegroundColor Cyan
Write-Host ""

$files = @(
  "preflight-demo.ps1",
  "generate-postman-collection.ps1",
  "generate-postman-collection-safe.ps1",
  "simulate-triage-vitals.ps1",
  "listen-ambulance-acks.ps1",
  "simulate-ambulance-mqtt.ps1",
  "resend-last-mqtt.ps1",
  "show-redis-telemetry.ps1"
)

$failed = $false

foreach ($name in $files) {
  $path = Join-Path $PSScriptRoot $name

  if (-not (Test-Path $path)) {
    Write-Host "[FAIL] ausente: $name" -ForegroundColor Red
    $failed = $true
    continue
  }

  $tokens = $null
  $errors = $null
  [System.Management.Automation.Language.Parser]::ParseFile(
    $path,
    [ref]$tokens,
    [ref]$errors
  ) | Out-Null

  if ($errors.Count -eq 0) {
    Write-Host "[OK]   $name" -ForegroundColor Green
  }
  else {
    Write-Host "[FAIL] $name" -ForegroundColor Red
    foreach ($error in $errors) {
      Write-Host ("       linha {0}: {1}" -f $error.Extent.StartLineNumber,$error.Message) -ForegroundColor Red
    }
    $failed = $true
  }
}

if ($failed) {
  throw "Existem erros estaticos no kit."
}

Write-Host ""
Write-Host "--- Gerando colecao Postman como teste ---" -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "generate-postman-collection-safe.ps1")

$collectionPath = Join-Path $PSScriptRoot "SUS-Smart-Care-Hackathon-Demo-FINAL.postman_collection.json"
$c = Get-Content -Raw $collectionPath | ConvertFrom-Json

function Count-Requests($Items) {
  $count = 0
  foreach ($item in $Items) {
    if ($null -ne $item.request) {
      $count++
    }
    if ($null -ne $item.item) {
      $count += Count-Requests $item.item
    }
  }
  return $count
}

$requestCount = Count-Requests $c.item

Write-Host ""
Write-Host "Colecao: $($c.info.name)" -ForegroundColor Cyan
Write-Host "Pastas principais: $($c.item.Count)"
Write-Host "Requests totais: $requestCount"

if ($requestCount -lt 70) {
  throw "Quantidade inesperadamente baixa de requests: $requestCount"
}

Write-Host ""
Write-Host "VALIDACAO ESTATICA OK." -ForegroundColor Green
Write-Host "Agora execute preflight-demo.ps1 para validar o ambiente real." -ForegroundColor Green
