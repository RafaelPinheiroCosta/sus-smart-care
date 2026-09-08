#!/usr/bin/env bash
set -euo pipefail
command -v mvn >/dev/null || { echo 'Maven 3.9+ não encontrado.'; exit 1; }
mvn -B -ntp clean verify
