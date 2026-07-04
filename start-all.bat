@echo off
chcp 936 >nul 2>&1
title Start All Services
cd /d "%~dp0"
if not exist logs mkdir logs

echo.
echo   Starting all services...
echo.

where node >nul 2>&1 || (echo   [!] Node.js not found & goto :fail)
where java >nul 2>&1 || (echo   [!] Java not found & goto :fail)

echo   [1] Backend      :8081
start /b cmd /c "cd backend && mvn spring-boot:run >> ../logs/backend.log 2>&1"
ping -n 31 127.0.0.1 >nul

echo   [2] Admin Web    :3001
start /b cmd /c "cd admin-web && npm run dev >> ../logs/admin-web.log 2>&1"
ping -n 6 127.0.0.1 >nul

echo   [3] Seller Web   :5173
start /b cmd /c "cd seller-web && npm run dev >> ../logs/seller-web.log 2>&1"
ping -n 6 127.0.0.1 >nul

echo   [4] Web Mall     :5176
start /b cmd /c "cd web-mall && npm run dev >> ../logs/web-mall.log 2>&1"

echo   All services started! Opening browsers...
echo.
ping -n 3 127.0.0.1 >nul
start http://localhost:3001
start http://localhost:5173
start http://localhost:5176
echo   Admin     http://localhost:3001   (admin / admin123)
echo   Seller    http://localhost:5173
echo   Mall      http://localhost:5176
echo   Swagger   http://localhost:8081/swagger-ui.html
echo.
echo   Logs: logs\   Stop: stop-all.bat
echo.
pause >nul & exit /b 0

:fail
echo.
echo   Please install missing dependencies first.
pause & exit /b 1
