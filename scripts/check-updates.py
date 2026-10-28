#!/usr/bin/env python3
"""Offline isolated updater policy/JUnit check. Never invokes Gradle."""
import glob
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
libs = glob.glob(str(Path.home() / '.gradle/wrapper/dists/gradle-*-bin/*/gradle-*/lib'))
if not libs:
    raise SystemExit('A locally installed Gradle distribution is needed for its Kotlin compiler jars (not a Gradle build).')
lib = Path(sorted(libs)[-1])
compiler = os.pathsep.join(str(p) for p in lib.glob('*.jar'))
stdlib = str(next(lib.glob('kotlin-stdlib-*.jar')))
junit = str(next(lib.glob('junit-4*.jar')))
hamcrest = str(next(lib.glob('hamcrest-core-*.jar')))
java_home = os.environ.get('JAVA_HOME')
java = str(Path(java_home) / 'bin/java') if java_home else str(next((Path.home() / '.local/share/panelin-android-toolchain').glob('jdk-17*/bin/java')))
source = ROOT / 'app/src/main/java/id/panelin/notifikasi/Updates.kt'
if not source.exists():
    raise SystemExit('RED: Updates.kt / UpdatePolicy missing')
text = source.read_text().split('// BEGIN PURE UPDATE POLICY\n')[1].split('// END PURE UPDATE POLICY')[0]
scratch = Path(os.environ.get('TMPDIR', str(Path.home() / '.hermes/cache/scratch')))
scratch.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='updates-policy-', dir=scratch) as directory:
    directory = Path(directory)
    policy = directory / 'Policy.kt'
    policy.write_text('package id.panelin.notifikasi\nimport java.net.URI\n' + text)
    classes = directory / 'classes'
    cp = os.pathsep.join([stdlib, junit, hamcrest])
    subprocess.run([java, '-cp', compiler, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-classpath', cp, '-d', str(classes), str(policy), str(ROOT / 'app/src/test/java/id/panelin/notifikasi/UpdatePolicyTest.kt')], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([str(classes), cp]), 'org.junit.runner.JUnitCore', 'id.panelin.notifikasi.UpdatePolicyTest'], check=True)
