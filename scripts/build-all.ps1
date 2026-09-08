$ErrorActionPreference = "Stop"
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
  throw "Maven não encontrado. Instale Maven 3.9+ ou execute pela IDE antes de usar este script."
}
mvn -B -ntp clean verify
