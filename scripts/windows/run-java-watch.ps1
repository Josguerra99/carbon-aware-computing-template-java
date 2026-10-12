#Requires -Version 5.1

function Start-JavaForecastWithWatch {
    param(
        [string]$Root,
        [string]$MvnExe,
        [int]$Port
    )

    $srcRoot = Join-Path $Root "src\main\java"
    if (-not (Test-Path $srcRoot)) {
        throw "Missing source folder: $srcRoot"
    }

    Write-Host "Starting forecast server (watch src/main/java) on port $Port ..."

    $state = @{
        Proc          = $null
        Restart       = $false
        LastEvent     = [datetime]::MinValue
        UserStop      = $false
    }

    function Stop-Server {
        if ($state.Proc -and -not $state.Proc.HasExited) {
            try {
                & taskkill.exe /PID $state.Proc.Id /T /F 2>$null | Out-Null
            } catch {
                Stop-Process -Id $state.Proc.Id -Force -ErrorAction SilentlyContinue
            }
        }
        $state.Proc = $null
    }

    function Start-Server {
        Stop-Server
        Write-Host "Compiling..."
        & $MvnExe -q compile
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Write-Host "Server running (Ctrl+C to stop). Edit Java under src/main/java to reload."
        $state.Proc = Start-Process -FilePath $MvnExe -ArgumentList @("-q", "exec:java") -WorkingDirectory $Root -PassThru
    }

    $handler = {
        $Event.MessageData.Restart = $true
        $Event.MessageData.LastEvent = Get-Date
    }

    $watcher = New-Object System.IO.FileSystemWatcher
    $watcher.Path = $srcRoot
    $watcher.IncludeSubdirectories = $true
    $watcher.EnableRaisingEvents = $true

    foreach ($evt in @("Changed", "Created", "Deleted", "Renamed")) {
        Register-ObjectEvent -InputObject $watcher -EventName $evt -Action $handler -MessageData $state | Out-Null
    }

    try {
        Start-Server
        while (-not $state.UserStop) {
            if ($state.Proc -and $state.Proc.HasExited) {
                if (-not $state.Restart) {
                    exit $state.Proc.ExitCode
                }
            }
            if ($state.Restart -and ((Get-Date) - $state.LastEvent).TotalMilliseconds -gt 800) {
                $state.Restart = $false
                Write-Host "Changes detected — restarting..."
                Start-Server
            }
            Start-Sleep -Milliseconds 250
        }
    } finally {
        $watcher.EnableRaisingEvents = $false
        Stop-Server
    }
}
