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
    # report healthy during that brief window. schema.sql also seeds ~1,100
    # bulk transactions via a row-by-row PL/pgSQL loop (for the 90-day
    # financial-identity history), which takes noticeably longer than the
    # handful of hand-written INSERTs - a single successful-but-early read
    # can catch the 20 hand-written rows before the bulk block finishes, and
    # `if ($output)` truthiness treats any non-empty result as "done" even
    # when the real target is still loading. So this polls the actual
    # transaction COUNT and requires it to clear a threshold safely below the
    # realistic minimum (~630 in the worst-case random draw) but far above
    # the 20-row false-positive, not just "the query returned something".
    Write-Host "Waiting for Postgres to finish initializing and loading the schema (seeding ~1,100+ rows can take a minute)..." -ForegroundColor Yellow
    $txnCount = 0
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 2
        try {
            $raw = docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c "SELECT count(*) FROM transactions;" 2>$null
        } catch {
            $raw = $null
        }
        $joined = (($raw -join "") -replace '\s', '')
        if ($LASTEXITCODE -eq 0 -and $joined -match '^\d+$') {
            $txnCount = [int]$joined
            if ($txnCount -gt 200) { break }
        }
    }
    $ErrorActionPreference = $prevEap

    if ($txnCount -le 200) {
        Write-Host "Postgres did not finish seeding in time (saw only $txnCount transactions) - check 'docker logs ukhonapay-postgres'." -ForegroundColor Red
        exit 1
    }

    # The transaction-count threshold above confirms the bulk seed finished,
    # but Postgres can still be mid-restart at this exact instant (same
    # transient window as before) - so this summary fetch gets its own short
    # retry rather than trusting a single attempt.
    $summary = ""
    $prevEap2 = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    for ($j = 0; $j -lt 5; $j++) {
        try {
            $raw2 = docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c `
                "SELECT (SELECT count(*) FROM users) || ' users, ' || (SELECT count(*) FROM vendors) || ' vendors, ' || (SELECT count(*) FROM transactions) || ' transactions, R' || (SELECT round(sum(amount)) FROM transactions) || ' volume';" 2>$null
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
