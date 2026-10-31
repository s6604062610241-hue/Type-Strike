"""Package portable play and source archives. Run after build and report generation."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
release = root / 'release'
release.mkdir(exist_ok=True)
source_roots = ['src', 'resources', 'tests', 'docs', 'tools', '.vscode']
source_files = ['.gitignore', 'README.md', 'Build.cmd', 'Play.cmd', 'build.ps1', 'run.ps1', 'test.ps1']
with ZipFile(release / 'TypeStrike-Source.zip', 'w', ZIP_DEFLATED) as archive:
    for folder in source_roots:
        for path in sorted((root / folder).rglob('*')):
            if path.is_file() and '__pycache__' not in path.parts:
                archive.write(path, 'TypeStrike/' + path.relative_to(root).as_posix())
    for name in source_files:
        archive.write(root / name, 'TypeStrike/' + name)

with ZipFile(release / 'TypeStrike-Windows.zip', 'w', ZIP_DEFLATED) as archive:
    archive.write(root / 'dist/TypeStrike.jar', 'TypeStrike/TypeStrike.jar')
    archive.writestr('TypeStrike/Play.cmd', '@echo off\r\ncd /d "%~dp0"\r\njava -jar "%~dp0TypeStrike.jar"\r\nif errorlevel 1 pause\r\n')
    archive.writestr('TypeStrike/README.txt',
        'Type Strike - Java OOP typing defense game\n\n'
        'Requires Java 21 or newer on Windows.\n'
        'Extract the entire ZIP, then double-click Play.cmd.\n'
        'You can also run: java -jar TypeStrike.jar\n\n'
        'Type an enemy word and press Enter. Escape pauses.\n'
        'Pink enemies: one word. Gold armored enemies: two different words.\n'
        'Scores are saved in %USERPROFILE%\\.type-strike\\scores.txt\n'
        'This archive does not include a Java runtime.\n')
for path in release.glob('*.zip'):
    print(path.name, path.stat().st_size, 'bytes')
