$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$jarPath = Join-Path $PSScriptRoot 'dist\TypeStrike.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { & (Join-Path $PSScriptRoot 'build.ps1') }
$javaPath = $null
if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { $javaPath = Join-Path $env:JAVA_HOME 'bin\java.exe' }
if (-not $javaPath) {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) { $javaPath = $javaCommand.Source }
}
if (-not $javaPath) { throw 'Java 21 or newer is required. Reopen your terminal after installing it.' }
& $javaPath -jar $jarPath
if ($LASTEXITCODE -ne 0) { throw 'The game could not run. Check your Java version.' }
