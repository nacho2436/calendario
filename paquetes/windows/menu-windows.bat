@echo off
rem === Mi Calendario: menu desplegable del servicio ===
rem Abre el panel con el menu desplegable (encender/apagar/abrir).
where pythonw >nul 2>nul
if %errorlevel%==0 (
  start "" pythonw "%~dp0menu_arranque.py"
  exit /b 0
)
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
start "" /min python "%~dp0menu_arranque.py"
