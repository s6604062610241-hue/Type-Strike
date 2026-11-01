"""Allow only game files in public uploads; preserve local academic documents unchanged.

Running this prepares an upload manifest in tmp/ and sanitized copies of historical
source packages. It does not upload, rewrite Git history, or modify old local packages.
"""
from hashlib import sha1, sha256
from pathlib import Path, PurePosixPath
from zipfile import ZipFile, ZIP_DEFLATED
import json

SOURCE_DIRS = ('src', 'resources', 'tests', '.vscode')
SOURCE_FILES = (
    '.gitignore', 'VERSION', 'README.md', 'Build.cmd', 'Play.cmd',
    'build.ps1', 'run.ps1', 'test.ps1', 'tools/package_release.py', 'tools/publication_files.py'
)

def allowed_source(name):
    path = PurePosixPath(name.replace('\\', '/'))
    parts = path.parts
    if not parts or path.is_absolute() or '..' in parts or '__pycache__' in parts:
        return False
    if parts[0] == 'TypeStrike':
        path = PurePosixPath(*parts[1:])
        parts = path.parts
    return bool(parts) and (parts[0] in SOURCE_DIRS or path.as_posix() in SOURCE_FILES)

def source_zip_without_documents(source, target, version):
    with ZipFile(source) as archive:
        if archive.testzip() is not None:
            raise ValueError('Damaged source archive: ' + str(source))
        bad = [n for n in archive.namelist() if not n.endswith('/') and not allowed_source(n)]
        if not bad:
            return source
        target.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(target, 'w', ZIP_DEFLATED) as cleaned:
            for item in archive.infolist():
                if item.is_dir() or not allowed_source(item.filename):
                    continue
                data = archive.read(item.filename)
                if PurePosixPath(item.filename).name == 'README.md' and len(PurePosixPath(item.filename).parts) == 2:
                    data = (f'# Type Strike {version}\n\nJava 21 Swing typing defense game.\n\n'
                            'Build: open Build.cmd. Play: open Play.cmd after building.\n'
                            'Tests: powershell -NoProfile -ExecutionPolicy Bypass -File test.ps1\n\n'
                            'Original game source is preserved. Academic documents are stored locally and excluded from this public source package.\n').encode()
                cleaned.writestr(item, data)
    with ZipFile(target) as archive:
        assert archive.testzip() is None
        assert all(allowed_source(n) for n in archive.namelist() if not n.endswith('/'))
        with ZipFile(source) as original:
            for name in archive.namelist():
                if '/src/' in name or '/resources/' in name or '/tests/' in name:
                    assert archive.read(name) == original.read(name), 'Historical game source was changed'
    return target

def prepare_publication(root, staging):
    root, staging = Path(root).resolve(), Path(staging).resolve()
    if not staging.is_relative_to(root / 'tmp'):
        raise ValueError('Publication staging must stay in the project tmp folder')
    result = {}
    for folder in SOURCE_DIRS:
        for path in sorted((root / folder).rglob('*')):
            if path.is_file() and not path.is_symlink() and '__pycache__' not in path.parts:
                result[path.relative_to(root).as_posix()] = path
    for name in SOURCE_FILES:
        path = root / name
        if path.is_file():
            result[name] = path
    descriptions = {
        'v1.0.0': 'Original game with normal and armored enemies.',
        'v1.1.0': 'Frost Slime, four-second Slow, one charge and the historical theme preview.',
        'v1.1.1': 'Gentler difficulty, five-second Slow, three charges and a full pause menu.',
        'v1.1.2': 'Same gameplay as v1.1.1, with more everyday vocabulary and game-only public packages.'
    }
    for version_dir in sorted((root / 'artifacts').iterdir()):
        if not version_dir.is_dir():
            continue
        version = version_dir.name
        prefix = 'artifacts/' + version + '/'
        for path in sorted(version_dir.rglob('*')):
            if not path.is_file():
                continue
            rel = path.relative_to(version_dir).as_posix()
            if rel.startswith('screenshots/') and path.suffix.lower() == '.png':
                result[prefix + rel] = path
            elif path.name in [f'TypeStrike-{version}.jar', f'TypeStrike-{version}-Windows.zip']:
                result[prefix + rel] = path
            elif path.name == f'TypeStrike-{version}-Source.zip':
                result[prefix + rel] = source_zip_without_documents(path, staging / (prefix + rel), version)
        readme = staging / (prefix + 'README.md')
        readme.parent.mkdir(parents=True, exist_ok=True)
        images = sorted(name for name in result if name.startswith(prefix + 'screenshots/'))
        text = (f'# Type Strike {version}\n\n' + descriptions.get(version, 'Java typing defense game.') +
                f'\n\n- [Source ZIP](TypeStrike-{version}-Source.zip)\n'
                f'- [Windows / Notebook ZIP](TypeStrike-{version}-Windows.zip)\n'
                f'- [Executable JAR](TypeStrike-{version}.jar)\n'
                '- [SHA256SUMS](SHA256SUMS.txt)\n\n'
                'Requires Java 21 or newer. Extract the Windows ZIP and open Play.cmd.\n'
                'Source: src/ in the repository or the Source ZIP. Build with Build.cmd.\n\n')
        text += '\n'.join(f'![{PurePosixPath(name).stem}]({name[len(prefix):]})\n' for name in images)
        readme.write_text(text, encoding='utf-8', newline='\n')
        result[prefix + 'README.md'] = readme
        sums = staging / (prefix + 'SHA256SUMS.txt')
        sums.write_text(''.join(sha256(path.read_bytes()).hexdigest() + '  ' + name[len(prefix):] + '\n'
                                for name, path in sorted(result.items()) if name.startswith(prefix)), encoding='ascii', newline='\n')
        result[prefix + 'SHA256SUMS.txt'] = sums
    return result

def write_manifest(root):
    root = Path(root).resolve()
    paths = prepare_publication(root, root / 'tmp/publication')
    manifest = []
    for name, path in sorted(paths.items()):
        raw = path.read_bytes()
        text = path.suffix.lower() not in ('.jar', '.zip', '.png', '.pdf')
        canonical = path.read_text(encoding='utf-8').encode() if text else raw
        manifest.append({'path': name, 'source': path.relative_to(root).as_posix(), 'size': len(raw), 'text': text,
                         'sha': sha1(('blob ' + str(len(canonical)) + '\0').encode() + canonical).hexdigest(),
                         'sha256': sha256(raw).hexdigest()})
    target = root / 'tmp/publication-manifest.json'
    target.write_text(json.dumps(manifest), encoding='utf-8')
    print(f'Prepared {len(manifest)} public game files; local documents and historical originals unchanged')

if __name__ == '__main__':
    write_manifest(Path(__file__).resolve().parents[1])
