"""Package the current source and playable version; never generate or change reports."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from publication_files import SOURCE_DIRS, SOURCE_FILES

root = Path(__file__).resolve().parents[1]
release = root / 'release'
release.mkdir(exist_ok=True)
version = (root / 'VERSION').read_text().strip()
prefix = 'TypeStrike-v' + version
source_roots = SOURCE_DIRS
source_files = SOURCE_FILES
with ZipFile(release / (prefix + '-Source.zip'), 'w', ZIP_DEFLATED) as archive:
    for folder in source_roots:
        for path in sorted((root / folder).rglob('*')):
            if path.is_file() and '__pycache__' not in path.parts:
                archive.write(path, 'TypeStrike/' + path.relative_to(root).as_posix())
    for name in source_files:
        archive.write(root / name, 'TypeStrike/' + name)

with ZipFile(release / (prefix + '-Windows.zip'), 'w', ZIP_DEFLATED) as archive:
    archive.write(root / 'dist/TypeStrike.jar', 'TypeStrike/TypeStrike.jar')
    archive.writestr('TypeStrike/Play.cmd', '@echo off\r\ncd /d "%~dp0"\r\njava -jar "%~dp0TypeStrike.jar"\r\nif errorlevel 1 pause\r\n')
    archive.writestr('TypeStrike/README.txt',
        'Type Strike - Java OOP typing defense game\n\n'
        'Requires Java 21 or newer on Windows.\n'
        'Extract the entire ZIP, then double-click Play.cmd.\n'
        'You can also run: java -jar TypeStrike.jar\n\n'
        'Type an enemy word and press Enter. Escape pauses.\n'
        'Normal enemies: one word. Armored enemies: two different words.\n'
        'Crystal Frost Slime: one word, +20 points and one Slow charge.\n'
        'Blast Slime: +15; radius 140. Kills unarmored enemies or breaks one armor layer.\n'
        'Blast chains and Frost rewards work. Blasts never hurt the tower.\n'
        'Healer Slime: +20; restores one nearby broken armor every 8 seconds (range 190).\n'
        'A dashed link warns 1.5 seconds before healing. Maximum one healer at a time.\n'
        'Restoring armor keeps the current word. Killing the healer stops its healing.\n'
        'Space: use Slow for 50% enemy movement for 5 seconds. Stores 3 charges.\n'
        'Slow affects movement only; healing/spawn clocks keep running. Pause stops all clocks.\n'
        'Escape: pause menu with Resume, Main Menu and Quit.\n'
        'Scores are saved in %USERPROFILE%\\.type-strike\\scores.txt\n'
        'This archive does not include a Java runtime.\n')
for path in release.glob(prefix + '*.zip'):
    print(path.name, path.stat().st_size, 'bytes')
