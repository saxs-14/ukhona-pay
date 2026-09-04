# Wipes the UKHONA PAY database and reloads DATABASE/schema.sql.
# Schema.sql no longer seeds any user/vendor/transaction data - every account
# is created through real signup. This just reloads the reference data (ATM
# locations, taxi associations, taxi ranks) the signup dropdowns need.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts/reset-demo-data.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "Stopping and wiping the UKHONA PAY database volume..." -ForegroundColor Yellow
Push-Location $root
try {
    docker compose down -v
    docker compose up -d

    # Postgres restarts itself once mid-init (it applies schema.sql against a
    # temporary server, then restarts as the real one) - pg_isready alone can
    # report healthy during that brief window. Poll the actual reference-data
    # count instead of just "the query returned something", since a blank
    # PowerShell array is still truthy.
    Write-Host "Waiting for Postgres to finish initializing and loading the schema..." -ForegroundColor Yellow
    $rankCount = 0
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        try {
            $raw = docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c "SELECT count(*) FROM taxi_ranks;" 2>$null
        } catch {
            $raw = $null
        }
        $joined = (($raw -join "") -replace '\s', '')
        if ($LASTEXITCODE -eq 0 -and $joined -match '^\d+$') {
            $rankCount = [int]$joined
            if ($rankCount -gt 0) { break }
        }
    }
    $ErrorActionPreference = $prevEap

    if ($rankCount -le 0) {
        Write-Host "Postgres did not finish loading the schema in time - check 'docker logs ukhonapay-postgres'." -ForegroundColor Red
        exit 1
    }

    $summary = ""
    $prevEap2 = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    for ($j = 0; $j -lt 5; $j++) {
        try {
            $raw2 = docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c `
                "SELECT (SELECT count(*) FROM taxi_associations) || ' taxi associations, ' || (SELECT count(*) FROM taxi_ranks) || ' taxi ranks, ' || (SELECT count(*) FROM atm_locations) || ' ATM locations, ' || (SELECT count(*) FROM users) || ' registered users';" 2>$null
        } catch {
            $raw2 = $null
        }
        $summary = ($raw2 -join "`n").Trim()
        if ($LASTEXITCODE -eq 0 -and $summary) { break }
        Start-Sleep -Seconds 2
    }
    $ErrorActionPreference = $prevEap2

    Write-Host "Reset complete: $summary" -ForegroundColor Green
    Write-Host "Restart the backend (mvn spring-boot:run) if it was already running, so its connection pool picks up the fresh database." -ForegroundColor Yellow
}
finally {
    Pop-Location
}
