#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
action="${1:-up}"
case "$action" in
  up) docker compose up -d ;;
  down) docker compose down ;;
  status) docker compose ps ;;
  *) echo "Uso: $0 {up|down|status}" >&2; exit 2 ;;
esac
