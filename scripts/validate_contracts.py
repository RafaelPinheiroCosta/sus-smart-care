from pathlib import Path
import sys,yaml
root=Path(__file__).resolve().parents[1]; errors=[]
for p in list((root/'contracts/openapi').glob('*.yaml')):
    try:
        d=yaml.safe_load(p.read_text());
        if not isinstance(d,dict) or not str(d.get('openapi','')).startswith('3.'): errors.append(f'{p}: openapi 3.x ausente')
        if not d.get('info') or not isinstance(d.get('paths'),dict): errors.append(f'{p}: info/paths inválidos')
    except Exception as e: errors.append(f'{p}: {e}')
for p in (root/'contracts/asyncapi').glob('*.yaml'):
    try:
        d=yaml.safe_load(p.read_text());
        if not str(d.get('asyncapi','')).startswith('3.'): errors.append(f'{p}: AsyncAPI 3.x ausente')
    except Exception as e: errors.append(f'{p}: {e}')
if errors:
 print('\n'.join(errors));sys.exit(1)
print('Contratos YAML validados estruturalmente.')
