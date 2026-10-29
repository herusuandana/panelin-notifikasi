#!/usr/bin/env python3
"""Run real pure forwarding and response policies without Android or network."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
lib = next((Path.home() / '.gradle/wrapper/dists').glob('gradle-*-bin/*/gradle-*/lib'))
java = str(next((Path.home() / '.local/share/panelin-android-toolchain').glob('jdk-17*/bin/java')))
compiler = os.pathsep.join(str(p) for p in lib.glob('*.jar'))
cp = os.pathsep.join(str(next(lib.glob(pattern))) for pattern in ['kotlin-stdlib-*.jar', 'junit-4*.jar', 'hamcrest-core-*.jar'])
with tempfile.TemporaryDirectory(prefix='forwarding-policy-', dir=Path.home() / '.hermes/cache/scratch') as directory:
    classes = str(Path(directory) / 'classes')
    sources = [root / 'app/src/main/java/id/panelin/notifikasi/Core.kt']
    tests = ['CoreTest', 'TracePolicyTest']
    sources += [root / f'app/src/test/java/id/panelin/notifikasi/{name}.kt' for name in tests]
    subprocess.run([java, '-cp', compiler, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-classpath', cp, '-d', classes, *map(str, sources)], check=True)
    subprocess.run([java, '-cp', classes + os.pathsep + cp, 'org.junit.runner.JUnitCore', *['id.panelin.notifikasi.' + name for name in tests]], check=True)
