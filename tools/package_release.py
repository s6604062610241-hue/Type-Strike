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
    archive.write(root / 'CHANGELOG.md', 'TypeStrike/CHANGELOG.md')
    archive.writestr('TypeStrike/Play.cmd', '@echo off\r\ncd /d "%~dp0"\r\njava -jar "%~dp0TypeStrike.jar"\r\nif errorlevel 1 pause\r\n')
    archive.writestr('TypeStrike/README.txt',
        'Type Strike - Java OOP typing defense game\n\n'
        'Requires Java 21 or newer on Windows.\n'
        'Extract the entire ZIP, then double-click Play.cmd.\n'
        'You can also run: java -jar TypeStrike.jar\n\n'
        'Type an enemy word and press Enter. Escape pauses.\n'
        'Normal enemies: one word. Armored enemies: two different words.\n'
        'Frost (Ice Zombie): one word, +20 points and one Slow charge.\n'
        'Blast Slime: +15; radius 140. Kills unarmored enemies or breaks one armor layer.\n'
        'Blast chains and Frost rewards work. Blasts never hurt the tower.\n'
        'Healer Slime: +20; restores one nearby broken armor every 8 seconds (range 190).\n'
        'A dashed link warns 1.5 seconds before healing. Maximum one healer at a time.\n'
        'Restoring armor keeps the current word. Killing the healer stops its healing.\n'
        'Space: use Slow for 50% enemy movement for 5 seconds. Stores 3 charges.\n'
        'Slow affects movement only; healing/spawn clocks keep running. Pause stops all clocks.\n'
        'Escape: pause menu with Resume, Main Menu and Quit.\n'
        'Pause menu: separate Mute/Unmute Music and SFX. Choices last until app closes.\n'
        'SFX: enemy defeat, armor break, tower damage, Slow and Blast; one cue per blast chain.\n'
        'SFX by Kenney, CC0; credits and original notices are bundled in audio/sfx/.\n'
        'Masked Orc switches to Orc Warrior when armor breaks and back when armor is restored.\n'
        'Rune Dome and gold/silver/bronze crowns use owner-selected ImageGen artwork.\n'
        'The wizard rests on the lower dome platform; decoration does not change collisions.\n'
        'See CHANGELOG.md for released versions and local development previews.\n'
        'Music pauses with the game; menu and battle tracks are bundled for offline play.\n'
        'Music by HydroGene, CC0: https://hydrogene.itch.io/high-quality-16-bit-music\n'
        'See audio/CREDITS.txt inside the JAR or resources/audio/CREDITS.txt in the source.\n'
        'Art by 0x72 (Robert), CC0: https://0x72.itch.io/dungeontileset-ii\n'
        'Animated sprites and dungeon backgrounds are bundled for offline play.\n'
        'See art/0x72/CREDITS.txt inside the JAR for art adaptations and sources.\n'
        'Pixelify Sans font is bundled; no font installation needed.\n'
        'Active font license: licenses/PixelifySans-OFL.txt (SIL Open Font License 1.1).\n'
        'Scores are saved in %USERPROFILE%\\.type-strike\\scores.txt\n'
        'This archive does not include a Java runtime.\n')
    archive.write(root / 'resources/fonts/OFL.txt', 'TypeStrike/licenses/PixelifySans-OFL.txt')
    archive.write(root / 'resources/fonts/CREDITS.txt', 'TypeStrike/licenses/PixelifySans-CREDITS.txt')
    archive.write(root / 'resources/fonts/VT323-OFL.txt', 'TypeStrike/licenses/VT323-OFL.txt')
    archive.write(root / 'resources/fonts/VT323-CREDITS.txt', 'TypeStrike/licenses/VT323-CREDITS.txt')
    for source, target in [
        ('resources/art/custom/CREDITS.txt', 'Custom-Art-CREDITS.txt'),
        ('resources/art/0x72/CREDITS.txt', '0x72-Art-CREDITS.txt'),
        ('resources/audio/CREDITS.txt', 'Music-CREDITS.txt'),
        ('resources/audio/sfx/CREDITS.txt', 'Kenney-SFX-CREDITS.txt'),
        ('resources/audio/sfx/Kenney-Interface-License.txt', 'Kenney-Interface-License.txt'),
        ('resources/audio/sfx/Kenney-Impact-License.txt', 'Kenney-Impact-License.txt')
    ]:
        archive.write(root / source, 'TypeStrike/licenses/' + target)
for path in release.glob(prefix + '*.zip'):
    print(path.name, path.stat().st_size, 'bytes')
