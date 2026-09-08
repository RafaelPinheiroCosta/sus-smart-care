param(
    [ValidateSet('up','down','status')]
    [string]$Action = 'up'
)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
switch ($Action) {
    'up'     { docker compose up -d }
    'down'   { docker compose down }
    'status' { docker compose ps }
}
