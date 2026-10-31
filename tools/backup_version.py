"""Create a verified, non-overwriting version snapshot using Python's standard library."""
import argparse
from datetime import datetime
from hashlib import sha256
from pathlib import Path
import re
import shutil
from zipfile import ZIP_DEFLATED, ZipFile

ROOT = Path(__file__).resolve().parents[1]
SKIP_ROOTS = {'versions', 'release', 'dist', 'build', 'tmp', '.git', '.agents', '.codex', '.aws'}
SKIP_NAMES = {'__pycache__', '.DS_Store'}

def digest(path):
    with path.open('rb') as stream:
        return sha256(stream.read()).hexdigest()

def backup(version):
    if not re.fullmatch(r'v\d+\.\d+\.\d+', version):
        raise SystemExit('Version must look like v1.0.0')
    versions = ROOT / 'versions'
    snapshot = versions / version
    archive_path = versions / f'TypeStrike-{version}-full-backup.zip'
    if snapshot.exists() or archive_path.exists():
        raise SystemExit(f'Refusing to overwrite existing backup: {version}')
    required = [ROOT / 'release/TypeStrike-Source.zip', ROOT / 'release/TypeStrike-Windows.zip',
                ROOT / 'dist/TypeStrike.jar', ROOT / 'docs/OOP-GUIDE.md',
                ROOT / 'docs/PRESENTATION.md', ROOT / 'docs/TypeStrike-Report-TH.pdf']
    missing = [str(path.relative_to(ROOT)) for path in required if not path.is_file()]
    if missing:
        raise SystemExit('Missing required artifacts: ' + ', '.join(missing))

    # Collect inputs before creating output, so the snapshot cannot include itself.
    inputs = []
    for child in sorted(ROOT.iterdir()):
        if child.name in SKIP_ROOTS or child.is_symlink():
            continue
        for path in ([child] if child.is_file() else sorted(child.rglob('*'))):
            if path.is_file() and not path.is_symlink() and not any(p in SKIP_NAMES for p in path.parts):
                inputs.append(path)
    snapshot.mkdir(parents=True, exist_ok=False)
    checks = {}
    for source in inputs:
        target = snapshot / 'project' / source.relative_to(ROOT)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        assert digest(source) == digest(target), f'Copy mismatch: {source}'

    packages = snapshot / 'packages'
    packages.mkdir()
    for source, name in [
        (required[0], f'TypeStrike-{version}-Source.zip'),
        (required[1], f'TypeStrike-{version}-Windows.zip'),
        (required[2], f'TypeStrike-{version}.jar')
    ]:
        target = packages / name
        shutil.copy2(source, target)
        assert digest(source) == digest(target)

    screenshots = snapshot / 'screenshots'
    screenshots.mkdir()
    captured = []
    for name in ['menu', 'gameplay', 'pause', 'gameover', 'menu-minimum']:
        source = ROOT / 'build/ui-review' / f'{name}.png'
        if not source.exists() and name == 'gameplay':
            source = ROOT / 'docs/images/gameplay.png'
        if source.is_file():
            shutil.copy2(source, screenshots / f'{name}.png')
            captured.append(name)

    stamp = datetime.now().astimezone().isoformat(timespec='seconds')
    info = f'''# Type Strike {version}

This is a preserved snapshot of Type Strike{' (the first playable version)' if version == 'v1.0.0' else ''}.
Version: {version}
Snapshot created: {stamp} (host clock; the version identifier is authoritative)

## Contents

- project/: source files, resources, tests, build/run scripts, VS Code configuration, tools and all existing docs.
- packages/TypeStrike-{version}-Source.zip: original source distribution.
- packages/TypeStrike-{version}-Windows.zip: portable notebook play distribution; requires Java 21+.
- packages/TypeStrike-{version}.jar: executable game JAR.
- project/docs/OOP-GUIDE.md: OOP explanation.
- project/docs/GAME-DESIGN.md: rules and limitations.
- project/docs/PRESENTATION.md: presentation notes (Markdown, not a slide deck).
- project/docs/TypeStrike-Report-TH.pdf, .md, .html: existing report and editable sources.
- project/docs/images/: report images.
- screenshots/: {', '.join(captured)}.
- SHA256SUMS.txt: verified hashes of all other files in this snapshot.

The gameplay and original release packages are unchanged. Version-management files were added to arrange this backup.
Build caches, temporary files, browser profiles and Git internals are excluded; all authored source and requested deliverables are included.
Do not edit this snapshot when developing later versions. Work in the main project folder.
The owner will write the academic report; future work focuses on the game unless documentation is explicitly requested.
This is a local backup, not an upload to GitHub.
'''
    (snapshot / 'VERSION-INFO.md').write_text(info, encoding='utf-8')
    for path in sorted(snapshot.rglob('*')):
        if path.is_file():
            checks[path.relative_to(snapshot).as_posix()] = digest(path)
    manifest = snapshot / 'SHA256SUMS.txt'
    manifest.write_text(''.join(f'{value}  {name}\n' for name, value in checks.items()), encoding='utf-8')

    with ZipFile(archive_path, 'w', ZIP_DEFLATED) as archive:
        for path in sorted(snapshot.rglob('*')):
            if path.is_file():
                archive.write(path, f'TypeStrike-{version}/' + path.relative_to(snapshot).as_posix())
    with ZipFile(archive_path) as archive:
        assert archive.testzip() is None, 'ZIP CRC verification failed'
        assert len(archive.namelist()) == len(checks) + 1
        for name, expected in checks.items():
            payload = archive.read(f'TypeStrike-{version}/' + name)
            assert sha256(payload).hexdigest() == expected, f'Archive mismatch: {name}'
    archive_hash = digest(archive_path)
    archive_path.with_suffix('.zip.sha256').write_text(f'{archive_hash}  {archive_path.name}\n', encoding='ascii')
    print(f'Created {snapshot}')
    print(f'Verified {len(checks)} files plus checksum manifest, {len(captured)} screenshots')
    print(f'Archive: {archive_path} ({archive_path.stat().st_size:,} bytes)')
    print(f'SHA256: {archive_hash}')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', required=True)
    backup(parser.parse_args().version)
