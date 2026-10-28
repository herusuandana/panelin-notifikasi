#!/usr/bin/env python3
"""Source guard for the wallet-inspired design, not visual/runtime QA."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
for feature in ['Brush.linearGradient', 'testTag("payment-overview")', 'testTag("quick-actions")']:
    assert feature in ui, f'Missing wallet design: {feature}'
for contract in ['Notifikasi pembayaran', 'Riwayat', 'Aplikasi', 'Pengaturan', 'Role.Tab', 'SecureFlagPolicy.SecureOn', 'PasswordVisualTransformation', 'consumeWindowInsets(padding)']:
    assert contract in ui, f'Missing existing UI contract: {contract}'
assert 'gopay' not in ui.lower(), 'Keep original Panelin branding'
print('Wallet design source guard OK. Runtime and screenshot checks remain required.')
