@echo off
rem === Mi Calendario: apagar el servidor ===
set "DATOS=%LOCALAPPDATA%\MiCalendario"
set MCALENDARIO_DATOS=%DATOS%
python "%~dp0servidor.py" stop
echo.
echo  El servidor quedo apagado. Cierra esta ventana.
timeout /t 4 >nul
