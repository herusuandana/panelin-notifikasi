#!/usr/bin/env python3
"""Build and verify a release APK; keep the update-signing identity outside the repo."""
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
subprocess.run([sys.executable, str(root / 'scripts/check-release.py')], check=True)
subprocess.run([sys.executable, str(root / 'scripts/check-startup.py')], check=True)
subprocess.run([sys.executable, str(root / 'scripts/check-ui-components.py')], check=True)
if hasattr(os, 'sched_getaffinity'):
    os.sched_setaffinity(0, sorted(os.sched_getaffinity(0))[-2:])
    os.nice(10)
base = Path.home() / '.local/share/panelin-android-toolchain'
jdk = Path(os.environ.get('JAVA_HOME') or next(base.glob('jdk-17*')))
sdk = Path(os.environ.get('ANDROID_HOME', base / 'android-sdk'))
private = Path.home() / '.local/share/panelin-android-signing'
private.mkdir(mode=0o700, parents=True, exist_ok=True)
private.chmod(0o700)
credentials = private / 'credentials.json'
keystore = private / 'panelin-release.jks'
if credentials.exists() != keystore.exists():
    sys.exit('Signing material incomplete. Restore the original key and credentials; refusing to replace the update identity.')
if not credentials.exists():
    password = secrets.token_urlsafe(40)
    values = {'storePassword': password, 'keyPassword': password}
    env = os.environ | {'PANELIN_STORE_PASSWORD': password, 'PANELIN_KEY_PASSWORD': password}
    subprocess.run([str(jdk / 'bin/keytool'), '-genkeypair', '-keystore', str(keystore), '-storetype', 'JKS', '-alias', 'panelin-release', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000', '-dname', 'CN=Panelin Android Release, O=Panelin, C=ID', '-storepass:env', 'PANELIN_STORE_PASSWORD', '-keypass:env', 'PANELIN_KEY_PASSWORD'], env=env, check=True)
    keystore.chmod(0o600)
    fd = os.open(credentials, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as stream:
        json.dump(values, stream)
values = json.loads(credentials.read_text())
env = os.environ | {'JAVA_HOME': str(jdk), 'ANDROID_HOME': str(sdk), 'ANDROID_SDK_ROOT': str(sdk), 'PANELIN_KEYSTORE': str(keystore), 'PANELIN_STORE_PASSWORD': values['storePassword'], 'PANELIN_KEY_PASSWORD': values['keyPassword']}
env['PATH'] = str(jdk / 'bin') + ':' + env['PATH']
wrapper = root / 'gradlew'
launcher = str(wrapper) if wrapper.exists() else str(base / 'gradle-8.11.1/bin/gradle')
subprocess.run([launcher, '--no-daemon', '--max-workers=2', 'testDebugUnitTest', 'lintRelease', 'assembleRelease', *sys.argv[1:]], cwd=root, env=env, check=True)
apk = root / 'app/build/outputs/apk/release/app-release.apk'
subprocess.run([str(sdk / 'build-tools/35.0.0/apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], env=env, check=True)
version = (root / 'VERSION').read_text().strip()
changes = json.loads((root / 'app/src/main/assets/changelog.json').read_text())
if changes[0]['version'] != version:
    sys.exit('VERSION differs from changelog; refusing to publish.')
badging = subprocess.run([str(sdk / 'build-tools/35.0.0/aapt'), 'dump', 'badging', str(apk)], capture_output=True, text=True, env=env, check=True).stdout
if f"versionName='{version}'" not in badging or f"versionCode='{changes[0]['code']}'" not in badging or "name='id.panelin.notifikasi'" not in badging:
    sys.exit('Built APK identity/version differs from release metadata; refusing to publish.')
output = root / 'dist'
output.mkdir(exist_ok=True)
name = f'panelin-notifikasi-{version}-release.apk'
target = output / name
shutil.copy2(apk, target)
digest = hashlib.sha256(target.read_bytes()).hexdigest()
(output / (name + '.sha256')).write_text(f'{digest}  {name}\n')
print('SIGNED APK:', target)
print('SHA256:', digest)
print('Keep an OFFLINE backup of signing identity:', private)
