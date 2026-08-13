@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\sauce-build.ps1" %*
exit /b %ERRORLEVEL%

