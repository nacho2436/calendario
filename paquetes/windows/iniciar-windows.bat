@echo off
setlocal
rem === Mi Calendario: encender el servidor ===
set "DATOS=%LOCALAPPDATA%\MiCalendario"
if not exist "%DATOS%" mkdir "%DATOS%"
where python >nul 2>nul
if errorlevel 1 (
  echo.
  echo  Se necesita Python 3. Descargalo gratis de:
  echo  https://www.python.org/downloads/
  echo  Al instalarlo marca la casilla "Add Python to PATH".
  start "" https://www.python.org/downloads/
  pause
  exit /b 1
)
set MCALENDARIO_DATOS=%DATOS%
start "MiCalendario servidor" /min python "%~dp0servidor.py"
timeout /t 2 /nobreak >nul
start "" http://127.0.0.1:8177
