#!/usr/bin/env python3
"""Start an isolated software-rendered test device; no production configuration."""
from pathlib import Path
import os
import subprocess

base = Path.home() / '.local/share/panelin-android-toolchain'
sdk = Path(os.environ.get('ANDROID_HOME', base / 'android-sdk'))
jdk = Path(os.environ.get('JAVA_HOME') or next(base.glob('jdk-17*')))
env = os.environ | {'JAVA_HOME': str(jdk), 'ANDROID_HOME': str(sdk), 'ANDROID_SDK_ROOT': str(sdk), 'ANDROID_AVD_HOME': str(base / 'avd')}
env['PATH'] = str(jdk / 'bin') + ':' + env['PATH']
Path(env['ANDROID_AVD_HOME']).mkdir(parents=True, exist_ok=True)
avd = sdk / 'cmdline-tools/cmdline-tools/bin/avdmanager'
name = 'PanelinLight'
if not (Path(env['ANDROID_AVD_HOME']) / (name + '.ini')).exists():
    subprocess.run([str(avd), 'create', 'avd', '-n', name, '-k', 'system-images;android-26;default;x86_64', '--device', 'Nexus 5'], input='no\n', text=True, env=env, check=True)
config = Path(env['ANDROID_AVD_HOME']) / (name + '.avd/config.ini')
text = config.read_text()
for key, value in {'hw.lcd.width': '720', 'hw.lcd.height': '1280', 'hw.lcd.density': '320'}.items():
    import re
    text = re.sub(r'^' + re.escape(key) + r'=.*$', key + '=' + value, text, flags=re.MULTILINE)
config.write_text(text)
if hasattr(os, 'sched_getaffinity'):
    os.sched_setaffinity(0, sorted(os.sched_getaffinity(0))[-4:-2])
    os.nice(10)
subprocess.run([str(sdk / 'emulator/emulator'), '-avd', name, '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot', '-gpu', 'swiftshader_indirect', '-accel', 'off', '-cores', '2', '-memory', '1536', '-no-metrics', '-port', '5580', '-feature', '-Vulkan'], env=env, check=True)
