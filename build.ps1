$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

function Find-Jdk {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { return $env:JAVA_HOME }
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { return (Split-Path (Split-Path $compiler.Source -Parent) -Parent) }
    $roots = @('C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java')
    foreach ($root in $roots) {
        if (Test-Path -LiteralPath $root) {
            $candidate = Get-ChildItem -LiteralPath $root -Filter javac.exe -Recurse -File | Select-Object -First 1
            if ($candidate) { return (Split-Path (Split-Path $candidate.FullName -Parent) -Parent) }
        }
    }
    throw 'JDK 21 or newer was not found. Install a JDK and set JAVA_HOME.'
}

$jdk = Find-Jdk
$classes = Join-Path $PSScriptRoot 'build\classes'
$dist = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Force -Path $classes, $dist | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& (Join-Path $jdk 'bin\javac.exe') --release 21 -encoding UTF-8 -Xlint:all,-serial -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\words.txt') -Destination $classes -Force
& (Join-Path $jdk 'bin\jar.exe') "-J-Djava.io.tmpdir=$dist" --create --file (Join-Path $dist 'TypeStrike.jar') --main-class typestrike.Main -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
Write-Host 'Build complete: dist\TypeStrike.jar' -ForegroundColor Green
