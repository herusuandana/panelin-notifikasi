#!/usr/bin/env python3
"""Bounded emulator readiness check."""
from pathlib import Path
import subprocess
import time

adb = Path.home() / '.local/share/panelin-android-toolchain/android-sdk/platform-tools/adb'
deadline = time.monotonic() + 600
while time.monotonic() < deadline:
    try:
        probe = subprocess.run([str(adb), '-s', 'emulator-5580', 'shell', 'getprop', 'sys.boot_completed'], capture_output=True, text=True, timeout=10)
        if probe.returncode == 0 and probe.stdout.strip() == '1':
            print('Emulator ready: emulator-5580')
            raise SystemExit(0)
    except subprocess.TimeoutExpired:
        pass
    time.sleep(10)
raise SystemExit('Emulator did not boot within readiness budget; no device test claimed.')
