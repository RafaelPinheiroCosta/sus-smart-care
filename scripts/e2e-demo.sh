#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
USERNAME="${USERNAME:-demo.admin}"
PASSWORD="${PASSWORD:-admin123}"
command -v jq >/dev/null || { echo 'jq é necessário para o E2E bash'; exit 1; }
TOKEN=$(curl -fsS -X POST "$KEYCLOAK_URL/realms/sus-smart-care/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode client_id=sus-smart-care-postman --data-urlencode grant_type=password \
  --data-urlencode username="$USERNAME" --data-urlencode password="$PASSWORD" | jq -r .access_token)
AUTH=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')
FACILITY_ID=$(python3 - <<<'import uuid; print(uuid.uuid4())')
PROFESSIONAL_ID=$(python3 - <<<'import uuid; print(uuid.uuid4())')
CPF="9$(date +%s%N | tail -c 11)"
api(){ curl -fsS "${AUTH[@]}" -H "X-Correlation-Id: $(python3 - <<<'import uuid; print(uuid.uuid4())')" "$@"; }
poll(){ local url="$1"; for _ in $(seq 1 30); do if out=$(api "$url" 2>/dev/null); then echo "$out"; return 0; fi; sleep 1; done; return 1; }
PATIENT=$(api -X POST "$BASE_URL/api/v1/patients" -d "{\"fullName\":\"Paciente Demo E2E\",\"birthDate\":\"1988-04-10\",\"identifierType\":\"CPF\",\"identifierValue\":\"$CPF\"}")
PATIENT_ID=$(jq -r .id <<<"$PATIENT")
VISIT=$(api -X POST "$BASE_URL/api/v1/pre-visits" -d "{\"patientId\":\"$PATIENT_ID\",\"facilityId\":\"$FACILITY_ID\",\"channel\":\"MOBILE\"}")
VISIT_ID=$(jq -r .id <<<"$VISIT")
api -X PUT "$BASE_URL/api/v1/pre-visits/$VISIT_ID/anamnesis" -d '{"text":"Pré-anamnese demonstrativa."}' >/dev/null
api -X POST "$BASE_URL/api/v1/pre-visits/$VISIT_ID/check-in" >/dev/null
TRIAGE=$(poll "$BASE_URL/api/v1/triages/by-visit/$VISIT_ID")
TRIAGE_ID=$(jq -r .id <<<"$TRIAGE")
DEVICE=$(api -X POST "$BASE_URL/api/v1/devices" -d "{\"externalId\":\"DEV-E2E-$(date +%s)\",\"deviceType\":\"MULTIPARAMETER_MONITOR\"}")
DEVICE_ID=$(jq -r .id <<<"$DEVICE")
SESSION=$(api -X POST "$BASE_URL/api/v1/telemetry/sessions" -d "{\"patientId\":\"$PATIENT_ID\",\"visitId\":\"$VISIT_ID\",\"sourceContext\":\"TRIAGE\"}")
SESSION_ID=$(jq -r .id <<<"$SESSION")
for spec in 'OXYGEN_SATURATION 94 %' 'HEART_RATE 108 bpm' 'BODY_TEMPERATURE 38.2 C'; do set -- $spec; api -X POST "$BASE_URL/api/v1/telemetry/sessions/$SESSION_ID/observations" -d "{\"deviceId\":\"$DEVICE_ID\",\"type\":\"$1\",\"value\":$2,\"unit\":\"$3\"}" >/dev/null; done
api -X POST "$BASE_URL/api/v1/triages/$TRIAGE_ID/assessment/ai" >/dev/null
api -X POST "$BASE_URL/api/v1/triages/$TRIAGE_ID/decision" -d "{\"priority\":\"HIGH\",\"professionalId\":\"$PROFESSIONAL_ID\"}" >/dev/null
QUEUE=$(poll "$BASE_URL/api/v1/queue-entries/by-visit/$VISIT_ID")
VIEW=$(poll "$BASE_URL/api/v1/clinical-view/visits/$VISIT_ID")
printf '%s\n' "$VIEW" | jq .
echo "E2E OK visit=$VISIT_ID queue=$(jq -r .displayCode <<<"$QUEUE")"
