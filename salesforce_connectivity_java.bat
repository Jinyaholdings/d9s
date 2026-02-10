@echo off
setlocal
set SCRIPT_DIR=%~dp0
set JAVA_CLASS=SalesforceConnectivity
set JAVA_CLASS_FILE=%SCRIPT_DIR%%JAVA_CLASS%.class
set JAVA_OPTS=
set SFDC_USERNAME=
set SFDC_PASSWORD=
set SFDC_API_VERSION=60.0
set SFDC_CLS_NAMESPACE=ps
set SFDC_OUTPUT_FILE=%SCRIPT_DIR%salesforce_connectivity.log

if not exist "%JAVA_CLASS_FILE%" (
  echo Missing class file: %JAVA_CLASS_FILE%
  echo Compile SalesforceConnectivity.java on a JDK 8+ and place the .class here.
  exit /b 1
)

if not "%SFDC_TLS_DEBUG%"=="" (
  set JAVA_OPTS=%JAVA_OPTS% -Djavax.net.debug=%SFDC_TLS_DEBUG%
)
if not "%SFDC_PREFER_IPV4%"=="" (
  set JAVA_OPTS=%JAVA_OPTS% -Djava.net.preferIPv4Stack=true
)
if not "%SFDC_PREFER_IPV6%"=="" (
  set JAVA_OPTS=%JAVA_OPTS% -Djava.net.preferIPv6Addresses=true
)

if "%SFDC_USERNAME%"=="" (
  echo SFDC_USERNAME is not set. Edit this BAT or set the environment variable.
  echo.
  echo Press any key to close...
  pause >nul
  exit /b 1
)
if "%SFDC_PASSWORD%"=="" (
  echo SFDC_PASSWORD is not set. Edit this BAT or set the environment variable.
  echo.
  echo Press any key to close...
  pause >nul
  exit /b 1
)

echo Running %JAVA_CLASS%...
java %JAVA_OPTS% %JAVA_CLASS%

echo.
echo Press any key to close...
pause >nul

endlocal
