from pathlib import Path
import re, sys, yaml, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
errors=[]

for p in root.rglob('pom.xml'):
    try: ET.parse(p)
    except Exception as e: errors.append(f'XML inválido {p.relative_to(root)}: {e}')

for p in list(root.rglob('*.yml'))+list(root.rglob('*.yaml')):
    try: list(yaml.safe_load_all(p.read_text(encoding='utf-8')))
    except Exception as e: errors.append(f'YAML inválido {p.relative_to(root)}: {e}')

for p in (root/'contracts/openapi').glob('*.yaml'):
    d=yaml.safe_load(p.read_text(encoding='utf-8'))
    if not str(d.get('openapi','')).startswith('3.'): errors.append(f'OpenAPI inválido {p.name}')
    if not isinstance(d.get('paths'),dict): errors.append(f'OpenAPI sem paths {p.name}')
for p in (root/'contracts/asyncapi').glob('*.yaml'):
    d=yaml.safe_load(p.read_text(encoding='utf-8'))
    if not str(d.get('asyncapi','')).startswith('3.'): errors.append(f'AsyncAPI inválido {p.name}')

for migration_dir in root.glob('**/src/main/resources/db/migration'):
    seen={}
    for p in migration_dir.glob('V*__*.sql'):
        m=re.match(r'V([^_]+)__',p.name)
        if not m: continue
        version=m.group(1)
        if version in seen: errors.append(f'Flyway versão duplicada em {migration_dir.relative_to(root)}: {seen[version].name} e {p.name}')
        seen[version]=p

# Outbox publica JSON já serializado: producer deve usar StringSerializer.
for cfg in root.glob('services/*/src/main/resources/application.yml'):
    text=cfg.read_text(encoding='utf-8')
    module=cfg.parents[3]
    outbox=module/'src/main/java'
    if any(outbox.rglob('OutboxPublisher.java')) and 'value-serializer: org.apache.kafka.common.serialization.StringSerializer' not in text:
        errors.append(f'{module.name}: Outbox exige Kafka StringSerializer')

# Evita regressão clínica acidental no provider de demonstração.
demo=root/'services/triage-service/src/main/java/br/com/sussmartcare/triage/infrastructure/ai/SafeDemoRiskAssessmentAdapter.java'
if demo.exists() and 'DEMO_ONLY_NO_CLINICAL_MODEL' not in demo.read_text(encoding='utf-8'):
    errors.append('Provider demo de IA perdeu o marcador explícito de não uso clínico')

if errors:
    print('\n'.join(f'ERRO: {e}' for e in errors)); sys.exit(1)
print('Static validation OK: XML, YAML, contratos, Flyway e invariantes técnicas.')
