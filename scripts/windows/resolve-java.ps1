#Requires -Version 5.1

function Test-JavaExe21 {
    param([string]$JavaExe)
    if (-not (Test-Path -LiteralPath $JavaExe)) { return $false }
    $versionText = (& $JavaExe -version 2>&1 | Out-String)
    if ($versionText -match 'version "(\d+)') {
        return [int]$Matches[1] -ge 21
    }
    return $false
}

function Get-WindowsJavaCandidates {
    $seen = [ordered]@{}
    function Add-Java([string]$Path) {
        if (-not $Path) { return }
        try {
            $full = (Resolve-Path -LiteralPath $Path -ErrorAction Stop).Path
            if (-not $seen.Contains($full)) { $seen[$full] = $true }
        } catch {
        }
    }

    if ($env:JAVA_HOME) {
        Add-Java (Join-Path $env:JAVA_HOME "bin\java.exe")
    }

    foreach ($pattern in @(
        "$env:ProgramFiles\Microsoft\jdk-*\bin\java.exe"
        "$env:ProgramFiles\Eclipse Adoptium\jdk-*\bin\java.exe"
        "$env:ProgramFiles\BellSoft\LibericaJDK-*\bin\java.exe"
        "$env:ProgramFiles\Java\jdk-*\bin\java.exe"
        "${env:ProgramFiles(x86)}\Java\jdk-*\bin\java.exe"
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-*\bin\java.exe"
    )) {
        Get-Item -Path $pattern -ErrorAction SilentlyContinue | ForEach-Object { Add-Java $_.FullName }
    }

    foreach ($hive in @(
        "HKLM:\SOFTWARE\Microsoft\JDK"
        "HKLM:\SOFTWARE\JavaSoft\JDK"
        "HKLM:\SOFTWARE\Eclipse Adoptium\JDK"
        "HKCU:\SOFTWARE\Microsoft\JDK"
    )) {
        if (-not (Test-Path $hive)) { continue }
        Get-ChildItem $hive -ErrorAction SilentlyContinue | ForEach-Object {
            $props = Get-ItemProperty -Path $_.PSPath -ErrorAction SilentlyContinue
            foreach ($name in @("Path", "JavaHome", "(default)")) {
                $jdkHome = $props.$name
                if ($jdkHome -and (Test-Path -LiteralPath $jdkHome)) {
                    Add-Java (Join-Path $jdkHome "bin\java.exe")
                }
            }
        }
    }

    @($seen.Keys)
}

function Find-JavaExe {
    if (Get-Command java -ErrorAction SilentlyContinue) {
        $cmd = (Get-Command java).Source
        if (Test-JavaExe21 $cmd) { return $cmd }
    }
    foreach ($candidate in Get-WindowsJavaCandidates) {
        if (Test-JavaExe21 $candidate) { return $candidate }
    }
    return $null
}

function Install-JavaWithWinget {
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        return $false
    }
    Write-Host "JDK 21+ not found. Installing Microsoft OpenJDK 21 with winget (may take a few minutes)..." -ForegroundColor Yellow
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & winget install --id Microsoft.OpenJDK.21 -e --accept-package-agreements --accept-source-agreements
    } finally {
        $ErrorActionPreference = $prev
    }
    return $LASTEXITCODE -eq 0
}

function Ensure-JavaForWorkshop {
    $javaExe = Find-JavaExe
    if (-not $javaExe) {
        if (Install-JavaWithWinget) {
            $javaExe = Find-JavaExe
        }
    }
    if (-not $javaExe) {
        Write-Host "JDK 21+ not found." -ForegroundColor Yellow
        Write-Host "Install: winget install Microsoft.OpenJDK.21  (or https://adoptium.net/)"
        Write-Host "Then open a new terminal and run setup again."
        throw "JDK 21 or newer is required."
    }

    $bin = Split-Path $javaExe -Parent
    $env:JAVA_HOME = Split-Path $bin -Parent
    if ($env:Path -notlike "*$bin*") {
        $env:Path = "$bin;$env:Path"
    }
    return $javaExe
}
