@echo off
chcp 936 >nul 2>&1
title Stop All Services

echo.
echo   Stopping all services...
echo.

for %%p in (8081 3001 5173 5176) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%p" ^| findstr "LISTENING" 2^>nul') do (
        taskkill /PID %%a /F >nul 2>&1
        echo   Stopped :%%p  PID=%%a
    )
)

echo.
echo   All services stopped.
echo.
pause
