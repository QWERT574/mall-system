@echo off
REM ============================================
REM MiniMall Docker Compose - Stop
REM Pure ASCII, encoding-safe
REM ============================================
chcp 65001 >nul 2>&1
title MiniMall - Docker Stop
cd /d "%~dp0"

echo.
echo   Stopping all services...
docker compose down

echo.
echo   Done.
echo   (Data volumes preserved. To clean: docker compose down -v)
echo.
pause >nul & exit /b 0
