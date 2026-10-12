#Requires -Version 5.1
$ErrorActionPreference = "Stop"

$javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $javaExe) {
    throw "java not found. Run scripts\windows\setup.cmd first."
}

$port = if ($env:PORT) { [int]$env:PORT } else { 3916 }

& (Join-Path $PSScriptRoot "ensure-lan-access.ps1") -ProgramPath $javaExe -ServerPort $port -Force
