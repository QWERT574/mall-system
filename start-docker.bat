@echo off
REM ============================================
REM MiniMall Docker Compose - Start
REM Pure ASCII, no Chinese chars (encoding-safe)
REM ============================================

REM Set UTF-8 for console output (echoes), but file content is pure ASCII
chcp 65001 >nul 2>&1
title MiniMall - Docker Start
cd /d "%~dp0"

echo.
echo ============================================
echo   MiniMall Docker Compose - Start
echo ============================================
echo.

REM Check .env file
if not exist .env (
    if exist .env.example (
        copy .env.example .env >nul
        echo   [!] .env not found, copied from .env.example
        echo   [!] Please edit .env with your real keys before production use.
        echo.
    ) else (
        echo   [X] .env.example missing, aborting.
        pause & exit /b 1
    )
)

REM Check Docker
where docker >nul 2>&1
if errorlevel 1 (
    echo   [X] Docker not found. Install Docker Desktop first:
    echo       https://www.docker.com/products/docker-desktop/
    pause & exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
    docker-compose --version >nul 2>&1
    if errorlevel 1 (
        echo   [X] docker compose not found.
        pause & exit /b 1
    )
)

echo   [1/4] Building backend image (first time takes 3-5 minutes)...
docker compose build backend
if errorlevel 1 goto :fail

echo.
echo   [2/4] Starting all services in background...
docker compose up -d
if errorlevel 1 goto :fail

echo.
echo   [3/4] Waiting for services to be healthy (max 90s)...
set /a wait=0
:wait_loop
set /a wait+=1
docker compose ps --format json 2>nul | findstr "\"Health\":\"healthy\"" >nul
if errorlevel 1 (
    if %wait% geq 18 (
        echo   [!] Timeout waiting. Run: docker compose ps
        goto :ready
    )
    echo       waiting... (%wait%/18)
    timeout /t 5 /nobreak >nul
    goto :wait_loop
)

:ready
echo.
echo   [4/4] Service status:
docker compose ps
echo.
echo ============================================
echo   All services ready!
echo ============================================
echo.
echo   Backend     http://localhost:8081
echo   Swagger     http://localhost:8081/swagger-ui/index.html
echo   Health      http://localhost:8081/actuator/health
echo   MySQL       localhost:3306  (root / see MYSQL_ROOT_PASSWORD in .env)
echo   Redis       localhost:6379
echo.
echo   Tail logs   :  docker compose logs -f
echo   Tail backend:  docker compose logs -f backend
echo   Stop        :  stop-docker.bat
echo   Reset       :  docker compose down -v   (also removes data volumes)
echo.
pause >nul & exit /b 0

:fail
echo.
echo   [X] Failed to start. Check output above.
pause & exit /b 1
