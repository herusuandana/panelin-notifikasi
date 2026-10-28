#!/usr/bin/env python3
"""One release contract check; no network and no production data."""
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
version = (root / 'VERSION').read_text().strip()
assert re.fullmatch(r'\d+\.\d+\.\d+', version), 'Invalid semantic version'
build = (root / 'app/build.gradle.kts').read_text()
code = int(re.search(r'versionCode\s*=\s*(\d+)', build)[1])
assert re.search(r'versionName\s*=\s*"' + re.escape(version) + r'"', build)
changes = json.loads((root / 'app/src/main/assets/changelog.json').read_text())
assert changes[0]['version'] == version and changes[0]['code'] == code
assert all(row['changes'] and all(isinstance(item, str) and item.strip() for item in row['changes']) for row in changes)
assert all(a['code'] > b['code'] for a, b in zip(changes, changes[1:])), 'versionCode must decrease through history'
assert f'## {version}' in (root / 'CHANGELOG.md').read_text()
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
a = '{http://schemas.android.com/apk/res/android}'
permissions = {p.attrib[a + 'name'] for p in manifest.findall('uses-permission')}
assert not any(any(part in p for part in ['READ_SMS', 'READ_CONTACTS', 'QUERY_ALL_PACKAGES', 'ACCESS_FINE_LOCATION', 'RECORD_AUDIO', 'MANAGE_EXTERNAL_STORAGE']) for p in permissions)
application = manifest.find('application')
assert application.attrib[a + 'allowBackup'] == 'false'
assert application.attrib[a + 'usesCleartextTraffic'] == 'false'
assert application.find('service').attrib[a + 'permission'] == 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'
assert not list(root.rglob('*.jks')) and not list(root.rglob('*.keystore')), 'Signing keys must remain outside source'
print(f'Release contracts OK: {version} ({code}), changelog, permissions, no bundled signing key')
