@echo off
setlocal
set SCRIPT_DIR=%~dp0
set JAVA_SRC=%SCRIPT_DIR%salesforce_connectivity.java
set JAVA_CLASS=SalesforceConnectivity

if not exist "%JAVA_SRC%" (
  echo Missing Java source: %JAVA_SRC%
  exit /b 1
)

echo Compiling %JAVA_CLASS%...
javac "%JAVA_SRC%"
if errorlevel 1 (
  echo javac failed.
  goto :end
)

echo Running %JAVA_CLASS%...
java %JAVA_CLASS%

echo.
echo Press any key to close...
pause >nul

:end
endlocal
