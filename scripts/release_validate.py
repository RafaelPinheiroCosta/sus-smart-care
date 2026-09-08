from pathlib import Path
import json, re, sys, yaml, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
errors=[]
expected='0.4.0-SNAPSHOT'
root_pom=(root/'pom.xml').read_text(encoding='utf-8')
if expected not in root_pom: errors.append('root pom não está em 0.4.0-SNAPSHOT')
for p in root.glob('services/*/pom.xml'):
    if expected not in p.read_text(encoding='utf-8'): errors.append(f'versão inconsistente: {p}')
for p in root.glob('platform/*/pom.xml'):
    if expected not in p.read_text(encoding='utf-8'): errors.append(f'versão inconsistente: {p}')
for p in root.glob('contracts/openapi/*.yaml'):
    d=yaml.safe_load(p.read_text(encoding='utf-8'))
    if d.get('info',{}).get('version')!='0.4.0': errors.append(f'OpenAPI versão inconsistente: {p.name}')
    schemas=d.get('components',{}).get('schemas',{})
    def walk(x):
      if isinstance(x,dict):
        ref=x.get('$ref')
        if isinstance(ref,str) and ref.startswith('#/components/schemas/') and ref.rsplit('/',1)[-1] not in schemas:
          errors.append(f'{p.name}: ref ausente {ref}')
        for v in x.values(): walk(v)
      elif isinstance(x,list):
        for v in x: walk(v)
    walk(d)
asyncapi=yaml.safe_load((root/'contracts/asyncapi/platform-events.yaml').read_text(encoding='utf-8'))
if asyncapi.get('info',{}).get('version')!='0.4.0': errors.append('AsyncAPI versão inconsistente')
full=yaml.safe_load((root/'docker-compose.full.yml').read_text(encoding='utf-8'))
for required in ['api-gateway','patient-registry-service','patient-journey-service','triage-service','queue-service','telemetry-service','presence-service','prehospital-service','notification-service','identity-access-service','clinical-query-service']:
    if required not in full.get('services',{}): errors.append(f'compose full sem {required}')
realm=json.loads((root/'infrastructure/keycloak/sus-smart-care-realm.json').read_text(encoding='utf-8'))
roles={r['name'] for r in realm.get('roles',{}).get('realm',[])}
for role in ['PATIENT','REPRESENTATIVE','TRIAGE_NURSE','DOCTOR','OPERATOR','ADMIN','AMBULANCE_TEAM','DEVICE']:
    if role not in roles: errors.append(f'Keycloak sem role {role}')
if errors:
    print('\n'.join('ERRO: '+e for e in errors)); sys.exit(1)
print('Release validation OK — v0.4 consistente.')
