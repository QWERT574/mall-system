@echo off
chcp 936 >nul 2>&1
echo.
echo   Restarting all services...
echo.
call stop-all.bat
ping -n 3 127.0.0.1 >nul
call start-all.bat
