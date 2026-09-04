# Wipes the UKHONA PAY database and reseeds it from DATABASE/schema.sql.
# Run this right before your real demo/pitch - every test payment, withdrawal,
# or rating you or a judge makes during rehearsal drifts the numbers away from
# the rehearsed narrative (5 vendors / 20 transactions / ~R3,670 volume).
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
    # report healthy during that brief window. Retry the real summary query
    # instead, since that only succeeds once the schema is fully loaded.
    Write-Host "Waiting for Postgres to finish initializing and loading the schema..." -ForegroundColor Yellow
    $counts = $null
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 2
        try {
            $output = docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c `
                "SELECT (SELECT count(*) FROM users) || ' users, ' || (SELECT count(*) FROM vendors) || ' vendors, ' || (SELECT count(*) FROM transactions) || ' transactions, R' || (SELECT round(sum(amount)) FROM transactions) || ' volume';" 2>$null
        } catch {
            $output = $null
        }
        # $output can arrive as a string[] (one element per line) even when every
        # line is blank - a non-empty array is truthy in PowerShell regardless of
        # its contents, so this join+trim is required, not cosmetic. A blank
        # result also occurs for one real reason: if this query runs in the
        # instant between schema creation and the seed INSERTs committing, the
        # SUM(amount) is still NULL and NULL propagates through the whole
        # concatenated string, printing an empty line that looks "successful".
        $joined = ($output -join "`n").Trim()
        if ($LASTEXITCODE -eq 0 -and $joined) {
            $counts = $joined
            break
        }
    }
    $ErrorActionPreference = $prevEap

    if (-not $counts) {
        Write-Host "Postgres did not finish initializing in time - check 'docker logs ukhonapay-postgres'." -ForegroundColor Red
        exit 1
    }

    Write-Host "Reset complete: $($counts.Trim())" -ForegroundColor Green
    Write-Host "Restart the backend (mvn spring-boot:run) if it was already running, so its connection pool picks up the fresh database." -ForegroundColor Yellow
}
finally {
    Pop-Location
}
