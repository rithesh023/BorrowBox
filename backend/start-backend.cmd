@echo off
setlocal

rem Use the Java and Maven Daemon installations already present on this PC.
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "MVND_HOME=%USERPROFILE%\Downloads\maven-mvnd-1.0.6-windows-amd64\maven-mvnd-1.0.6-windows-amd64"
set "PORT=8900"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Java was not found at "%JAVA_HOME%".
  echo Update JAVA_HOME near the top of this file to your installed JDK folder.
  pause
  exit /b 1
)

if not exist "%MVND_HOME%\mvn\bin\mvn.cmd" (
  echo The Maven command bundled with Maven Daemon was not found at "%MVND_HOME%\mvn\bin\mvn.cmd".
  echo Update MVND_HOME near the top of this file to the extracted Maven Daemon folder.
  pause
  exit /b 1
)

set "PATH=%MVND_HOME%\bin;%JAVA_HOME%\bin;%PATH%"
set "BACKEND_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":8900 .*LISTENING"') do set "BACKEND_PID=%%P"

if defined BACKEND_PID (
  powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; try { $r=Invoke-RestMethod -Uri 'http://127.0.0.1:8900/api/health' -TimeoutSec 3; if ($r.application -eq 'BorrowBox' -and $r.status -eq 'ok') { exit 0 } else { exit 1 } } catch { exit 1 }"
  if errorlevel 1 (
    echo Port 8900 is occupied by process %BACKEND_PID%, but it did not return BorrowBox health.
    tasklist /FI "PID eq %BACKEND_PID%"
    echo Stop that process or choose another port before starting BorrowBox.
    pause
    exit /b 1
  )
  if /I "%~1"=="restart" (
    call :stop_existing
    if errorlevel 1 exit /b 1
    goto start_backend
  )
  echo BorrowBox backend is already running and healthy at http://localhost:8900/api/health
  echo Use "%~nx0 restart" when you want to restart it.
  exit /b 0
)

:start_backend
cd /d "%~dp0"
echo Starting BorrowBox backend on port 8900...
call "%MVND_HOME%\mvn\bin\mvn.cmd" spring-boot:run
set "START_RESULT=%ERRORLEVEL%"
if not "%START_RESULT%"=="0" echo BorrowBox stopped with exit code %START_RESULT%.
exit /b %START_RESULT%

:stop_existing
echo Stopping BorrowBox process %BACKEND_PID%...
taskkill /PID %BACKEND_PID% /T /F >nul
if errorlevel 1 (
  echo Could not stop process %BACKEND_PID%. Close its terminal manually and retry.
  exit /b 1
)
set "WAIT_SECONDS=0"
:wait_for_port_release
set "BACKEND_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":8900 .*LISTENING"') do set "BACKEND_PID=%%P"
if not defined BACKEND_PID exit /b 0
timeout /t 1 /nobreak >nul
set /a WAIT_SECONDS+=1
if %WAIT_SECONDS% LSS 20 goto wait_for_port_release
echo Port 8900 did not become free. The backend was stopped but could not be restarted.
exit /b 1
