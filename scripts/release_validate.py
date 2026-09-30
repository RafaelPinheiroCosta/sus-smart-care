from pathlib import Path
import json
import re
import sys
import yaml
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []

# Maven: a versão dos módulos deve seguir a versão do parent/root.
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
try:
    root_xml = ET.parse(root / "pom.xml").getroot()
    root_version = root_xml.findtext("m:version", namespaces=ns)
except Exception as exc:
    errors.append(f"pom raiz inválido: {exc}")
    root_version = None

for p in list(root.glob("services/*/pom.xml")) + list(root.glob("platform/*/pom.xml")):
    try:
        x = ET.parse(p).getroot()
        parent = x.find("m:parent", ns)
        parent_version = parent.findtext("m:version", namespaces=ns) if parent is not None else None
        if root_version and parent_version != root_version:
            errors.append(f"versão do parent inconsistente: {p.relative_to(root)} ({parent_version} != {root_version})")
    except Exception as exc:
        errors.append(f"pom inválido {p.relative_to(root)}: {exc}")

# Contratos: cada boundary evolui de forma independente; exigimos somente versão semântica e refs válidas.
semver = re.compile(r"^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$")
for p in root.glob("contracts/openapi/*.yaml"):
    try:
        d = yaml.safe_load(p.read_text(encoding="utf-8"))
        version = str(d.get("info", {}).get("version", ""))
        if not semver.match(version):
            errors.append(f"OpenAPI sem versão semântica: {p.name}")
        schemas = d.get("components", {}).get("schemas", {})
        def walk(x):
            if isinstance(x, dict):
                ref = x.get("$ref")
                if isinstance(ref, str) and ref.startswith("#/components/schemas/"):
                    if ref.rsplit("/", 1)[-1] not in schemas:
                        errors.append(f"{p.name}: ref ausente {ref}")
                for v in x.values():
                    walk(v)
            elif isinstance(x, list):
                for v in x:
                    walk(v)
        walk(d)
    except Exception as exc:
        errors.append(f"OpenAPI inválido {p.name}: {exc}")

for p in root.glob("contracts/asyncapi/*.yaml"):
    try:
        d = yaml.safe_load(p.read_text(encoding="utf-8"))
        version = str(d.get("info", {}).get("version", ""))
        if not semver.match(version):
            errors.append(f"AsyncAPI sem versão semântica: {p.name}")
    except Exception as exc:
        errors.append(f"AsyncAPI inválido {p.name}: {exc}")

# Stack completa: as 12 aplicações do MVP devem estar presentes.
try:
    full = yaml.safe_load((root / "docker-compose.full.yml").read_text(encoding="utf-8"))
    services = full.get("services", {})
    required = [
        "api-gateway",
        "patient-registry-service",
        "patient-journey-service",
        "triage-service",
        "queue-service",
        "telemetry-service",
        "presence-service",
        "prehospital-service",
        "notification-service",
        "identity-access-service",
        "clinical-query-service",
        "facility-service",
    ]
    for name in required:
        if name not in services:
            errors.append(f"compose full sem {name}")
    if "mosquitto" not in services:
        errors.append("compose full sem broker MQTT mosquitto")
except Exception as exc:
    errors.append(f"docker-compose.full.yml inválido: {exc}")

# Contratos obrigatórios da evolução IoT/facility.
for required in [
    root / "contracts/openapi/facility.yaml",
    root / "contracts/openapi/telemetry.yaml",
    root / "contracts/asyncapi/telemetry-mqtt.yaml",
]:
    if not required.exists():
        errors.append(f"contrato obrigatório ausente: {required.relative_to(root)}")

# Roles esperadas no realm local.
try:
    realm = json.loads((root / "infrastructure/keycloak/sus-smart-care-realm.json").read_text(encoding="utf-8"))
    roles = {r["name"] for r in realm.get("roles", {}).get("realm", [])}
    for role in ["PATIENT", "REPRESENTATIVE", "TRIAGE_NURSE", "DOCTOR", "OPERATOR", "ADMIN", "AMBULANCE_TEAM", "DEVICE"]:
        if role not in roles:
            errors.append(f"Keycloak sem role {role}")
except Exception as exc:
    errors.append(f"realm Keycloak inválido: {exc}")

if errors:
    print("\n".join("ERRO: " + e for e in errors))
    sys.exit(1)

print("Release validation OK — 12 aplicações, contratos versionados e integrações finais presentes.")
