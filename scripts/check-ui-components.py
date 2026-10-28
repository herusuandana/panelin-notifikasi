#!/usr/bin/env python3
"""Guard Compose-only app UI; runtime semantics are tested separately."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
assert 'import androidx.activity.compose.setContent' in source, 'UI must use Compose'
assert 'setContent {' in source
for stock in ['Spinner(', 'ArrayAdapter(', 'AlertDialog.Builder', 'LinearLayout(', 'Toast.makeText', 'AndroidView(']:
    assert stock not in source, f'Legacy app surface: {stock}'
for requirement in ['FLAG_SECURE', 'PasswordVisualTransformation', 'remember { mutableStateOf', 'ModalBottomSheet', 'LazyColumn', 'IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS', 'window.decorView.isSaveEnabled = false', 'consumeWindowInsets(padding)', 'view.rootView.setFilterTouchesWhenObscured(true)', 'view.rootView.importantForAutofill']:
    assert requirement in source, requirement
for requirement in ['testTag(tag)', 'role=Role.Button', 'confirmValueChange={ it != SheetValue.Hidden || canDismiss }', '!wide && !keyboardVisible']:
    assert requirement in source, requirement
assert 'contentDescription=tag' not in source, 'Internal test IDs must not become spoken labels'
print('Compose UI surface contract OK; runtime tests remain required.')
