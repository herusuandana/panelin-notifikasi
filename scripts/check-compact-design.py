#!/usr/bin/env python3
"""Compact Compose source regression checks, not runtime or visual evidence."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
for feature in ['PullToRefreshBox(', 'rememberPullToRefreshState()', 'isRefreshing=refreshing',
                'InitialSkeleton()', 'if(initialLoading)', 'LinearProgressIndicator(',
                'Role.RadioButton', 'selectableGroup()', 'getApplicationIcon(pkg)',
                'Dispatchers.IO', 'iconCache', 'internal fun AppIcon(',
                'testTag("payment-overview")', 'testTag("quick-actions")',
                'testTag("refresh-$page")', 'testTag("monitoring-status")']:
    assert feature in ui, f'Missing compact UI behavior: {feature}'
assert 'Brush.linearGradient' not in ui, 'Use neutral compact cards, not wallet gradients'
assert not re.search(r'fontSize=(?:30|34|56)\.sp', ui), 'Oversized wallet typography remains'
home = ui.split('0 -> {', 1)[1].split('1 -> {', 1)[0]
for duplicate in ['Shortcut(', 'SettingRow(', 'Kelola aplikasi', 'Akses cepat', 'Kredit tercatat']:
    assert duplicate not in home, f'Duplicated home feature: {duplicate}'
for metric in ['Dalam antrean', 'Perlu perhatian', 'Terakhir diterima', 'Terakhir dikirim']:
    assert home.count('"'+metric+'"') == 1, f'Home metric must appear once: {metric}'
history = ui.split('1 -> {', 1)[1].split('2 -> {', 1)[0]
assert 'AppIcon(d.packageName' in history, 'History must show the actual package icon'
assert 'Glyph(1,' not in history, 'A clock is not an app icon'
assert 'heightIn(min=48.dp)' in ui, 'Keep accessible touch targets'
for contract in ['Notifikasi pembayaran', 'Riwayat pengiriman', 'Aplikasi terhubung',
                 'Pengaturan aplikasi', 'Role.Tab', 'SecureFlagPolicy.SecureOn',
                 'PasswordVisualTransformation', 'consumeWindowInsets(padding)',
                 'view.rootView.importantForAutofill', 'confirmValueChange={ it != SheetValue.Hidden || canDismiss }']:
    assert contract in ui, f'Missing existing UI contract: {contract}'
assert 'Action("Tutup") { detail=null }' not in ui, 'Detail sheet already has a close action'
print('Compact UI source guard OK. Build, instrumentation and visual QA remain required.')
