@echo off
setlocal
cd /d "%~dp0..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\setup-wsl-gpu.ps1 %*
exit /b %ERRORLEVEL%
