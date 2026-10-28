#!/usr/bin/env python3
"""Responsive Compose checks on the disposable emulator, never a client phone."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
adb = Path.home() / '.local/share/panelin-android-toolchain/android-sdk/platform-tools/adb'
base = [str(adb), '-s', 'emulator-5580']
output = root / 'dist/qa' / ('trace-' + (root / 'VERSION').read_text().strip()) / 'responsive'
output.mkdir(parents=True, exist_ok=True)

def run(*args, timeout=180):
    return subprocess.run(base + list(args), capture_output=True, text=True, timeout=timeout, check=True).stdout

size = run('shell', 'wm', 'size')
density = run('shell', 'wm', 'density')
font = run('shell', 'settings', 'get', 'system', 'font_scale').strip()
(output / 'original-config.txt').write_text(size + density + 'font_scale=' + font + '\n')
failures = []
try:
    for name, pixels, dpi, scale in [('phone', '720x1280', '320', '1.0'), ('compact-large-text', '640x1136', '320', '1.6'), ('landscape', '1280x720', '320', '1.0'), ('tablet', '1000x800', '160', '1.0')]:
        run('shell', 'am', 'force-stop', 'id.panelin.notifikasi')
        run('shell', 'wm', 'size', pixels)
        run('shell', 'wm', 'density', dpi)
        run('shell', 'settings', 'put', 'system', 'font_scale', scale)
        config = run('shell', 'wm', 'size') + run('shell', 'wm', 'density') + 'font_scale=' + run('shell', 'settings', 'get', 'system', 'font_scale')
        (output / (name + '-config.txt')).write_text(config)
        results = []
        for test, count in [('ResponsiveTest', 1), ('EditorSecurityTest', 1), ('TraceUiTest', 3)]:
            run('shell', 'pm', 'clear', 'id.panelin.notifikasi')
            try:
                result = run('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', 'id.panelin.notifikasi.' + test, 'id.panelin.notifikasi.test/androidx.test.runner.AndroidJUnitRunner', timeout=600)
            except (subprocess.TimeoutExpired, subprocess.CalledProcessError) as error:
                result = error.stdout or ''
                if isinstance(result, bytes):
                    result = result.decode(errors='replace')
                result += '\nHARNESS ERROR: configuration incomplete\n' + str(error)
            (output / (name + '-' + test + '.txt')).write_text(result)
            results.append(result)
            passed = re.search(r'OK \((\d+) tests?\)', result)
            if not passed or int(passed[1]) != count or 'FAILURES' in result:
                failures.append(name + '/' + test)
        (output / (name + '.txt')).write_text('\n'.join(results))
        if any(failure.startswith(name + '/') for failure in failures):
            failures.append(name)
        print(name, 'PASS (5 tests)' if name not in failures else 'FAILED', flush=True)
finally:
    for key, value in [('size', size), ('density', density)]:
        override = re.search(r'Override (?:size|density): (\S+)', value)
        run('shell', 'wm', key, override[1] if override else 'reset')
    if font in ('null', ''):
        run('shell', 'settings', 'delete', 'system', 'font_scale')
    else:
        run('shell', 'settings', 'put', 'system', 'font_scale', font)
    run('shell', 'am', 'force-stop', 'id.panelin.notifikasi')
if failures:
    raise SystemExit('Failed responsive configurations: ' + ', '.join(failures))
print('Responsive matrix: 20 tests passed. Original display configuration restored.')
