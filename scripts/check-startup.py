#!/usr/bin/env python3
"""Guard the window initialization order; run StartupTest for runtime coverage."""
from pathlib import Path

source = (Path(__file__).resolve().parents[1] / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
startup = source.split('override fun onCreate(savedInstanceState: Bundle?)', 1)[1].split('override fun onResume()', 1)[0]
assert startup.index('setContentView(root)') < startup.index('window.insetsController'), \
    'Window decor must exist before PhoneWindow.getInsetsController (Android 11+)'
print('Startup initialization order OK; this check does not replace Android runtime tests.')
