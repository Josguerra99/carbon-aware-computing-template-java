#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $Root

Write-Host "=== Carbon-aware template (Java) - Windows setup ===" -ForegroundColor Cyan
Write-Host "Installs: JDK 21+ (check), Maven (PATH or .tools), mvn dependency:resolve."
Write-Host "Before run: set MAIN_SERVER_URL in .env to the URL from your facilitator."
Write-Host ""

& (Join-Path $PSScriptRoot "../ensure-env.ps1")
. (Join-Path $PSScriptRoot "resolve-java.ps1")

$javaExe = Ensure-JavaForWorkshop
Write-Host "Java: $((Invoke-JavaVersionOutput -JavaExe $javaExe | Select-Object -First 1))"

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
$prevEa = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$mvnVersionLine = (& $mvn -version 2>&1 | Select-Object -First 1)
$ErrorActionPreference = $prevEa
Write-Host "Maven: $mvnVersionLine"

Write-Host "Resolving dependencies..."
& $mvn -q -DskipTests dependency:resolve compile

Write-Host ""
Write-Host "Setup complete. Run: scripts\windows\run.cmd" -ForegroundColor Green
