@echo off
setlocal
set SCRIPT_DIR=%~dp0
set JAVA_CLASS=SalesforceConnectivity
set JAVA_CLASS_FILE=%SCRIPT_DIR%%JAVA_CLASS%.class

if not exist "%JAVA_CLASS_FILE%" (
  echo Missing class file: %JAVA_CLASS_FILE%
  echo Compile SalesforceConnectivity.java on a JDK 8+ and place the .class here.
  exit /b 1
)

echo Running %JAVA_CLASS%...
java %JAVA_CLASS%

echo.
echo Press any key to close...
pause >nul

endlocal
