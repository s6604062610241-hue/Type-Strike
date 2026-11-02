$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
. (Join-Path $PSScriptRoot 'build.ps1')
$jdk = Find-Jdk
$testClasses = Join-Path $PSScriptRoot 'build\test-classes'
New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tests') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& (Join-Path $jdk 'bin\javac.exe') --release 21 -encoding UTF-8 -cp 'build\classes' -d $testClasses @testSources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp "build\classes;$testClasses" typestrike.GameRulesTest
if ($LASTEXITCODE -ne 0) { throw 'Game tests failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp "build\classes;$testClasses" typestrike.SlowPowerUpTest
if ($LASTEXITCODE -ne 0) { throw 'Slow power-up tests failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp "build\classes;$testClasses" typestrike.DifficultyBalanceTest
if ($LASTEXITCODE -ne 0) { throw 'Difficulty balance tests failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp "build\classes;$testClasses" typestrike.SpecialEnemiesTest
if ($LASTEXITCODE -ne 0) { throw 'Special enemy tests failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp "build\classes;$testClasses" typestrike.MusicPlayerTest
if ($LASTEXITCODE -ne 0) { throw 'Music tests failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp "build\classes;$testClasses" typestrike.ui.ArtResourcesTest
if ($LASTEXITCODE -ne 0) { throw 'Art resource tests failed.' }
