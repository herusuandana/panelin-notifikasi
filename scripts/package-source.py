#!/usr/bin/env python3
"""Package editable source, never local SDK paths, secrets, caches or test data."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parents[1]
version = (root / 'VERSION').read_text().strip()
output = root / 'dist'
output.mkdir(exist_ok=True)
archive = output / f'panelin-notifikasi-{version}-source.zip'
excluded_dirs = {'.git', '.gradle', '.kotlin', '.idea', 'build', 'dist'}
excluded_names = {'local.properties', 'signing.properties'}
with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED) as stream:
    for path in sorted(root.rglob('*')):
        relative = path.relative_to(root)
        if not path.is_file() or any(part in excluded_dirs for part in relative.parts) or path.name in excluded_names:
            continue
        if path.suffix.lower() in {'.jks', '.keystore', '.hprof'}:
            raise SystemExit(f'Refusing to package signing material: {relative}')
        stream.write(path, Path(root.name) / relative)
with zipfile.ZipFile(archive) as stream:
    assert stream.testzip() is None
    assert not any(name.endswith(('local.properties', '.jks', '.keystore')) for name in stream.namelist())
(output / (archive.name + '.sha256')).write_text(f'{hashlib.sha256(archive.read_bytes()).hexdigest()}  {archive.name}\n')
print('Source archive:', archive)
