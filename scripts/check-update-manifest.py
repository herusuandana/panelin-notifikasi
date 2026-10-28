#!/usr/bin/env python3
"""Guard the deliberately narrow update installer surface, not runtime behavior."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
a = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
assert any(p.get(a + 'name') == 'android.permission.REQUEST_INSTALL_PACKAGES' for p in manifest.findall('uses-permission')), 'Missing explicit user-confirmed update permission'
providers = manifest.findall('application/provider')
assert len(providers) == 1, 'One narrow APK provider only'
provider = providers[0]
assert provider.get(a + 'name') == 'androidx.core.content.FileProvider'
assert provider.get(a + 'authorities') == '${applicationId}.updates'
assert provider.get(a + 'exported') == 'false'
assert provider.get(a + 'grantUriPermissions') == 'true'
metadata = provider.find('meta-data')
assert metadata.get(a + 'name') == 'android.support.FILE_PROVIDER_PATHS'
assert metadata.get(a + 'resource') == '@xml/update_paths'
paths = ET.parse(root / 'app/src/main/res/xml/update_paths.xml').getroot()
assert len(paths) == 1 and paths[0].tag == 'files-path'
assert paths[0].get('name') == 'updates' and paths[0].get('path') == 'updates/'
print('Update manifest: user confirmation required, private narrow file provider OK.')
