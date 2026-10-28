#!/usr/bin/env python3
"""Keep app copy short. Runtime layout checks are separate."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
for name in ['MainActivity.kt', 'HelpText.kt', 'UiRules.kt', 'DeliveryWorker.kt', 'ForwarderService.kt']:
    source = (root / 'app/src/main/java/id/panelin/notifikasi' / name).read_text()
    for value in re.findall(r'"((?:\\.|[^"\\])*)"', source):
        if value == 'application/json; charset=utf-8':
            continue  # Protocol value, not UI copy.
        assert ';' not in value, f'{name}: replace semicolon in copy: {value}'
        assert not any(word in value for word in ['NOTIFICATION HUB', 'OVERVIEW', 'CONNECTED APPS', 'Kendali tetap di perangkat']), value
assert 'Notifikasi pembayaran' in (root / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
print('UI copy OK: plain Indonesian, no semicolons.')
