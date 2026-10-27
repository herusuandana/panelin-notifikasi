#!/usr/bin/env python3
"""Guard the window initialization order; run StartupTest for runtime coverage."""
from pathlib import Path

source = (Path(__file__).resolve().parents[1] / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
assert 'window.insetsController' not in source, 'Use AndroidX edge-to-edge, not the raw decor controller'
assert 'enableEdgeToEdge()' in source and 'setContent { PanelTheme { App() } }' in source
assert 'WindowInsets.safeDrawing' in source and '.imePadding()' in source
print('Compose startup/insets source contract OK; Android runtime tests remain required.')
