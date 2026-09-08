$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
python scripts/static_validate.py
python scripts/validate_contracts.py
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    mvn -B -ntp verify
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Warning 'Maven não encontrado; validação Maven não executada.'
}
