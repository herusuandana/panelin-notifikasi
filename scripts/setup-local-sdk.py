#!/usr/bin/env python3
"""Install SDK packages into the dedicated local toolchain, not system paths."""
from pathlib import Path
import os
import subprocess

base = Path.home() / '.local/share/panelin-android-toolchain'
jdk = next(base.glob('jdk-17*'))
sdk = base / 'android-sdk'
env = os.environ | {'JAVA_HOME': str(jdk), 'ANDROID_HOME': str(sdk), 'ANDROID_SDK_ROOT': str(sdk)}
env['PATH'] = str(jdk / 'bin') + ':' + env['PATH']
manager = sdk / 'cmdline-tools/cmdline-tools/bin/sdkmanager'
subprocess.run([str(manager), f'--sdk_root={sdk}', '--licenses'], input='y\n' * 100, text=True, env=env, check=True)
subprocess.run([str(manager), f'--sdk_root={sdk}', 'platform-tools', 'platforms;android-35', 'build-tools;35.0.0', 'emulator', 'system-images;android-26;default;x86_64'], input='y\n' * 100, text=True, env=env, check=True)
print('SDK ready:', sdk)
