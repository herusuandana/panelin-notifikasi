#!/usr/bin/env python3
"""Exercise real Android UI and Keystore/SQLite on the isolated emulator."""
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
adb = Path.home() / '.local/share/panelin-android-toolchain/android-sdk/platform-tools/adb'
output = root / 'dist/qa'
output.mkdir(parents=True, exist_ok=True)
base = [str(adb), '-s', 'emulator-5580']
def run(*arguments, timeout=600):
    print('ADB:', arguments[0], flush=True)
    result = subprocess.run(base + list(arguments), capture_output=True, text=True, timeout=timeout)
    print(result.stdout, result.stderr, flush=True)
    if result.returncode:
        raise SystemExit(result.returncode)
    return result.stdout

subprocess.run([sys.executable, str(root / 'scripts/wait-device.py')], check=True)
run('shell', 'settings', 'put', 'global', 'window_animation_scale', '0')
run('shell', 'settings', 'put', 'global', 'transition_animation_scale', '0')
run('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0')
run('shell', 'input', 'keyevent', '82')
run('install', '-r', str(root / 'app/build/outputs/apk/debug/app-debug.apk'))
run('install', '-r', str(root / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'))
for name in ['StartupTest', 'StoreSecurityTest', 'QueueBehaviorTest', 'StatusNotificationTest', 'WorkerContractTest', 'ClientSmokeTest']:
    run('shell', 'pm', 'clear', 'id.panelin.notifikasi')
    result = run('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', f'id.panelin.notifikasi.{name}', 'id.panelin.notifikasi.test/androidx.test.runner.AndroidJUnitRunner', timeout=300)
    (output / f'{name}.txt').write_text(result)
    if 'FAILURES' in result or 'INSTRUMENTATION_FAILED' in result or 'OK (' not in result:
        raise SystemExit(f'{name} did not pass')
    if name == 'ClientSmokeTest':
        run('pull', '/sdcard/Android/data/id.panelin.notifikasi/files/.', str(output))
run('shell', 'pm', 'clear', 'id.panelin.notifikasi')
# The distributed release uses its permanent certificate, not the debug identity.
run('uninstall', 'id.panelin.notifikasi.test')
run('uninstall', 'id.panelin.notifikasi')
version = (root / 'VERSION').read_text().strip()
run('install', str(root / f'dist/panelin-notifikasi-{version}-release.apk'))
run('shell', 'am', 'start', '-W', '-n', 'id.panelin.notifikasi/.MainActivity')
print('Device checks passed; fixture data cleared; signed release installed.', flush=True)
