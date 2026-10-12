#Requires -Version 5.1
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RemainingArgs
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $Root

$fixPermissions = $false
foreach ($a in @($RemainingArgs)) {
    if ($a -eq "--fix-permissions" -or $a -eq "-FixPermissions") {
        $fixPermissions = $true
    }
}

& (Join-Path $PSScriptRoot "../ensure-env.ps1")
. (Join-Path $PSScriptRoot "resolve-java.ps1")
Ensure-JavaForWorkshop | Out-Null

$MavenVersion = "3.9.10"
$PortableMaven = Join-Path $Root ".tools\apache-maven-$MavenVersion\bin\mvn.cmd"
$mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" }
       elseif (Test-Path $PortableMaven) { $PortableMaven }
       else { $null }
if (-not $mvn) {
    Write-Host "Maven not ready - running setup..."
    & (Join-Path $PSScriptRoot "setup.ps1")
    $mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } else { $PortableMaven }
}

if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -gt 0) {
            $key = $line.Substring(0, $eq).Trim()
            $val = $line.Substring($eq + 1).Trim()
            Set-Item -Path "Env:$key" -Value $val
        }
    }
}

$port = if ($env:PORT) { $env:PORT } else { "3916" }

if ($fixPermissions) {
    $javaExe = (Get-Command java).Source
    & (Join-Path $PSScriptRoot "ensure-lan-access.ps1") -ProgramPath $javaExe -ServerPort ([int]$port) -Force
}

Write-Host "Starting forecast server (Maven exec:java) on port $port..."
& $mvn -q exec:java
