#!/usr/bin/env python3
"""Check the installed signed release on the disposable emulator only."""
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
adb = Path.home() / '.local/share/panelin-android-toolchain/android-sdk/platform-tools/adb'
base = [str(adb), '-s', 'emulator-5580']
output = root / 'dist/qa' / ('trace-' + (root / 'VERSION').read_text().strip()) / 'release'
output.mkdir(parents=True, exist_ok=True)

def run(*arguments):
    return subprocess.run(base + list(arguments), capture_output=True, timeout=45, check=True).stdout

def tree():
    run('shell', 'rm', '-f', '/sdcard/release.xml')
    result = run('shell', 'uiautomator', 'dump', '/sdcard/release.xml')
    assert b'dumped to' in result, result.decode(errors='replace')
    return ET.fromstring(run('shell', 'cat', '/sdcard/release.xml'))

package = run('shell', 'dumpsys', 'package', 'id.panelin.notifikasi').decode()
assert 'DEBUGGABLE' not in package, 'Install the signed non-debuggable release before this check.'
version = (root / 'VERSION').read_text().strip()
assert f'versionName={version}' in package, 'Installed release does not match current source version.'
run('shell', 'am', 'start', '-W', '-n', 'id.panelin.notifikasi/.MainActivity')

expected = {'Riwayat': 'Riwayat pengiriman', 'Aplikasi': 'Aplikasi terhubung',
            'Pengaturan': 'Pengaturan aplikasi', 'Beranda': 'Notifikasi pembayaran'}
for tab, heading in expected.items():
    nodes = [node for node in tree().iter('node') if node.attrib.get('text') == tab]
    assert nodes, f'Missing navigation: {tab}'
    bounds = list(map(int, re.findall(r'\d+', nodes[-1].attrib['bounds'])))
    run('shell', 'input', 'tap', str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
    assert any(node.attrib.get('text') == heading for node in tree().iter('node')), f'Missing page: {tab}'
    (output / f'release-{tab}.png').write_bytes(run('exec-out', 'screencap', '-p'))
    print('PASS signed release tab', tab, flush=True)
