param([string]$OutputPath = "")

$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "generate-postman-collection.ps1"
if (-not (Test-Path $sourcePath)) {
  throw "Fonte da colecao nao encontrada: $sourcePath"
}
if (-not $OutputPath) {
  $OutputPath = Join-Path $PSScriptRoot "SUS-Smart-Care-Hackathon-Demo-FINAL.postman_collection.json"
}

# Le a fonte e aplica somente normalizacoes de compatibilidade antes de executa-la.
$source = Get-Content -Raw $sourcePath

# OrderedDictionary e Hashtable sao ambos IDictionary. Isso evita dependencia
# de conversao implicita no PowerShell ao montar os formularios de login.
$source = $source.Replace(
  '[hashtable]$Form=$null',
  '[System.Collections.IDictionary]$Form=$null'
)

$temp = Join-Path $env:TEMP ("ssc-postman-" + [guid]::NewGuid().ToString("N") + ".ps1")
try {
  Set-Content -Path $temp -Value $source -Encoding UTF8

  # Valida sintaxe PowerShell do gerador normalizado antes de executa-lo.
  $tokens = $null
  $errors = $null
  [System.Management.Automation.Language.Parser]::ParseFile(
    $temp,
    [ref]$tokens,
    [ref]$errors
  ) | Out-Null

  if ($errors.Count -gt 0) {
    $errors | ForEach-Object {
      Write-Host ("[ERRO] linha {0}: {1}" -f $_.Extent.StartLineNumber,$_.Message) -ForegroundColor Red
    }
    throw "Gerador Postman possui erro de sintaxe."
  }

  & $temp -OutputPath $OutputPath

  # Segunda validacao: JSON parseavel + estrutura minima da colecao.
  $c = Get-Content -Raw $OutputPath | ConvertFrom-Json
  if (-not $c.info -or -not $c.item -or $c.item.Count -lt 6) {
    throw "Colecao gerada, mas estrutura minima nao foi encontrada."
  }

  $expected = @(
    '00 - AUTENTICACAO E PREPARACAO',
    '01 - LUCAS CRIANCA - JORNADA COMPLETA',
    '02 - LUCAS FUTURO - REPRESENTACAO PARA SELF',
    '03 - SEM SMARTPHONE - DONA ROSA',
    '04 - AMBULANCIA + MQTT + REDIS',
    '05 - OBSERVABILIDADE E SEGURANCA'
  )

  $actual = @($c.item | ForEach-Object { $_.name })
  foreach ($folder in $expected) {
    if ($actual -notcontains $folder) {
      throw "Pasta obrigatoria ausente na colecao: $folder"
    }
  }

  Write-Host ""
  Write-Host "OK - colecao pronta para importar no Postman:" -ForegroundColor Green
  Write-Host "  $OutputPath"
  Write-Host "Pastas principais: $($c.item.Count)"
}
finally {
  Remove-Item $temp -ErrorAction SilentlyContinue
}
