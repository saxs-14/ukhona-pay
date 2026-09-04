# Run this before you go on stage. Checks the exact things that broke during
# development (Docker not running, port 5432 conflict, backend/frontend down)
# so you find out now, not mid-pitch.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts/preflight-check.ps1

$fail = $false

function Check($label, [scriptblock]$test) {
    try {
        $result = & $test
        if ($result) {
            Write-Host "[OK]   $label" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] $label" -ForegroundColor Red
            $script:fail = $true
        }
    } catch {
        Write-Host "[FAIL] $label - $($_.Exception.Message)" -ForegroundColor Red
        $script:fail = $true
    }
}

Write-Host "UKHONA PAY preflight check" -ForegroundColor Cyan
Write-Host "--------------------------"

Check "Docker Desktop is running" { docker info *> $null; $LASTEXITCODE -eq 0 }
Check "ukhonapay-postgres container is up" { (docker ps --format "{{.Names}}" | Select-String "ukhonapay-postgres") -ne $null }
Check "Postgres accepting connections on 5442" { docker exec ukhonapay-postgres pg_isready -U ukhonapay *> $null; $LASTEXITCODE -eq 0 }
Check "Schema loaded (taxi_ranks table queryable)" {
    docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c "SELECT count(*) FROM taxi_ranks;" *> $null
    $LASTEXITCODE -eq 0
}
Check "Backend responding on :8080" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/taxi-associations" -UseBasicParsing -TimeoutSec 5
        $r.StatusCode -eq 200
    } catch { $false }
}
Check "Frontend responding on :5173" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:5173/" -UseBasicParsing -TimeoutSec 5
        $r.StatusCode -eq 200
    } catch { $false }
}
Check "No native Postgres process squatting on port 5442" {
    $conn = Get-NetTCPConnection -LocalPort 5442 -ErrorAction SilentlyContinue
    $offenders = $conn | Where-Object { (Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName -match "postgres" }
    $offenders.Count -eq 0
}

Write-Host "--------------------------"
if ($fail) {
    Write-Host "One or more checks FAILED. See README.md Troubleshooting section." -ForegroundColor Red
    Write-Host "Quick fixes: 'docker compose up -d' for DB, 'cd BACKEND; mvn spring-boot:run' for API, 'cd FRONTEND; npm run dev' for UI." -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "All checks passed. Good to go." -ForegroundColor Green
}
