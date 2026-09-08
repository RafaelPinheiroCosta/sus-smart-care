from pathlib import Path
import xml.etree.ElementTree as ET, yaml, sys
root=Path(__file__).resolve().parents[1]
errors=[]
for p in root.rglob('pom.xml'):
    try: ET.parse(p)
    except Exception as e: errors.append(f'{p}: {e}')
for p in list(root.rglob('*.yml'))+list(root.rglob('*.yaml')):
    try: list(yaml.safe_load_all(p.read_text(encoding='utf-8')))
    except Exception as e: errors.append(f'{p}: {e}')
print('OK' if not errors else '\n'.join(errors)); sys.exit(1 if errors else 0)
