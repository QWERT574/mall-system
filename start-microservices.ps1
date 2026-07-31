# ============================================
#  MiniMall microservices batch start script
#  Usage: powershell -ExecutionPolicy Bypass -File start-microservices.ps1
#  Requires: MySQL(13306) + Nacos(8848) already running
#  Credentials: loaded from .env (copy .env.example first); no secrets hardcoded here
# ============================================
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$logDir = Join-Path $root 'logs'
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }

# ---- Load .env into process env vars (KEY=VALUE lines, # comments ignored) ----
$envFile = Join-Path $root '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$' -and $_ -notmatch '^\s*#') {
            [System.Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim(), 'Process')
        }
    }
    Write-Host '  Loaded env vars from .env' -ForegroundColor DarkGray
} else {
    Write-Host '  [WARN] .env not found - copy .env.example to .env and fill in values' -ForegroundColor Yellow
}

# ---- Required vars check (fail fast instead of starting broken services) ----
if (-not $env:JWT_SECRET) {
    Write-Host '  [ERROR] JWT_SECRET is not set (required). Aborting.' -ForegroundColor Red
    exit 1
}

# ---- Local-run defaults (only set when .env did not provide them) ----
if (-not $env:JWT_EXPIRATION) { $env:JWT_EXPIRATION = '86400000' }
if (-not $env:DB_HOST)        { $env:DB_HOST        = 'localhost' }
if (-not $env:DB_PORT)        { $env:DB_PORT        = '13306' }
if (-not $env:DB_USERNAME)    { $env:DB_USERNAME    = 'root' }
if (-not $env:DB_PASSWORD)    { $env:DB_PASSWORD    = $env:MYSQL_ROOT_PASSWORD }
if (-not $env:DB_NAME)        { $env:DB_NAME        = 'minimall' }
if (-not $env:NACOS_HOST)     { $env:NACOS_HOST     = 'localhost' }
if (-not $env:REDIS_HOST)     { $env:REDIS_HOST     = 'localhost' }
if (-not $env:REDIS_PORT)     { $env:REDIS_PORT     = '6379' }

# ---- Service list (order: core data -> business -> gateway) ----
$services = @(
    @{Name='user-service';    Port=8081},
    @{Name='product-service'; Port=8082},
    @{Name='order-service';   Port=8083},
    @{Name='payment-service'; Port=8084},
    @{Name='chat-service';    Port=8085},
    @{Name='ai-service';      Port=8086},
    @{Name='gateway';         Port=8080}
)

Write-Host ''
Write-Host '  Starting MiniMall microservices...' -ForegroundColor Cyan
Write-Host ''

foreach ($svc in $services) {
    $svcDir = Join-Path $root $svc.Name
    if (-not (Test-Path (Join-Path $svcDir 'pom.xml'))) {
        Write-Host "  [SKIP] $($svc.Name) : pom.xml not found" -ForegroundColor Yellow
        continue
    }
    $logFile  = Join-Path $logDir "$($svc.Name).log"
    $errFile  = Join-Path $logDir "$($svc.Name).err"
    # Launch mvn via cmd /c with log redirection; process detaches from this session
    $cmd = "mvn spring-boot:run > `"$logFile`" 2> `"$errFile`""
    Start-Process -FilePath 'cmd.exe' -ArgumentList "/c $cmd" -WorkingDirectory $svcDir -WindowStyle Hidden
    Write-Host ("  [START] {0,-16} port {1}" -f $svc.Name, $svc.Port) -ForegroundColor Green
    Start-Sleep -Seconds 6
}

Write-Host ''
Write-Host "  All services launched. Logs: $logDir" -ForegroundColor Cyan
Write-Host '  Use stop-microservices.ps1 to stop.' -ForegroundColor Cyan
Write-Host ''
