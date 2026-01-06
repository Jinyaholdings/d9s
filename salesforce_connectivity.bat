@echo off
setlocal
set SCRIPT_DIR=%~dp0
set PS_SCRIPT=%SCRIPT_DIR%salesforce_diag.ps1

if not exist "%PS_SCRIPT%" (
  echo Missing PowerShell script: %PS_SCRIPT%
  exit /b 1
)

echo Running Salesforce connectivity diagnostics...
PowerShell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%"

endlocal
