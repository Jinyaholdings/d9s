@echo off
setlocal
set TARGET=login.salesforce.com

echo Running Salesforce connectivity diagnostics (BAT-only)...
echo Start: %date% %time%
echo.

echo ==== DNS (nslookup) ====
nslookup %TARGET%
echo.

echo ==== ICMP (ping) ====
ping -n 4 %TARGET%
echo.

echo ==== Route (tracert, max 5 hops) ====
tracert -d -h 5 %TARGET%
echo.

echo ==== HTTPS (curl or certutil) ====
where curl >nul 2>nul
if %errorlevel%==0 (
  curl -I https://%TARGET%/
  goto :done_https
)

where certutil >nul 2>nul
if %errorlevel%==0 (
  rem Downloads to temp cache and validates TLS
  certutil -urlcache -split -f https://%TARGET%/ NUL
  goto :done_https
)

echo curl/certutil not found; HTTPS check skipped.

:done_https
echo.
echo End: %date% %time%
echo.
Pause

endlocal

