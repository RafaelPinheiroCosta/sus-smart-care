#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python scripts/static_validate.py
python scripts/validate_contracts.py
if command -v mvn >/dev/null 2>&1; then
  mvn -B -ntp verify
else
  echo "[WARN] Maven não encontrado; validação Maven não executada." >&2
fi
