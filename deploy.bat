@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
if /i "%~1"=="h5" (
    shift
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%deploy.ps1" -OnlyH5 %*
    exit /b %ERRORLEVEL%
)
if /i "%~1"=="api" (
    shift
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%deploy.ps1" -OnlyApi %*
    exit /b %ERRORLEVEL%
)
if /i "%~1"=="backend" (
    shift
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%deploy.ps1" -OnlyApi %*
    exit /b %ERRORLEVEL%
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%deploy.ps1" %*

exit /b %ERRORLEVEL%
