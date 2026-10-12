#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $Root

Write-Host "=== Carbon-aware template (Java) - Windows setup ===" -ForegroundColor Cyan
Write-Host "Installs: JDK 21+ (check), Maven (PATH or .tools), mvn dependency:resolve."
Write-Host "Before run: set MAIN_SERVER_URL in .env to the URL from your facilitator."
Write-Host ""

& (Join-Path $PSScriptRoot "../ensure-env.ps1")

function Test-Java21 {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { return $false }
    $versionText = (java -version 2>&1 | Out-String)
    if ($versionText -match 'version "(\d+)') {
        return [int]$Matches[1] -ge 21
    }
    return $false
}

if (-not (Test-Java21)) {
    Write-Host "Java 21+ not found on PATH." -ForegroundColor Yellow
    Write-Host "Install: winget install Microsoft.OpenJDK.21  (or https://adoptium.net/)"
    throw "JDK 21 or newer is required."
}
Write-Host "Java: $(java -version 2>&1 | Select-Object -First 1)"

$MavenVersion = "3.9.10"
$ToolsDir = Join-Path $Root ".tools"
$PortableMaven = Join-Path $ToolsDir "apache-maven-$MavenVersion\bin\mvn.cmd"

function Get-MvnCommand {
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        return "mvn"
    }
    if (Test-Path $PortableMaven) {
        return $PortableMaven
    }
    Write-Host "Downloading Apache Maven $MavenVersion to .tools ..."
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
    $zip = Join-Path $env:TEMP "apache-maven-$MavenVersion-bin.zip"
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip" -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $ToolsDir -Force
    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    if (-not (Test-Path $PortableMaven)) {
        throw "Maven download failed."
    }
    return $PortableMaven
}

$mvn = Get-MvnCommand
Write-Host "Maven: $(& $mvn -version 2>&1 | Select-Object -First 1)"

Write-Host "Resolving dependencies..."
& $mvn -q -DskipTests dependency:resolve compile

Write-Host ""
Write-Host "Setup complete. Run: powershell -ExecutionPolicy Bypass -File scripts/windows/run.ps1" -ForegroundColor Green
