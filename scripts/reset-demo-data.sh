#!/usr/bin/env bash
# Wipes the UKHONA PAY database and reseeds it from DATABASE/schema.sql.
# Run this right before your real demo/pitch - every test payment, withdrawal,
# or rating you or a judge makes during rehearsal drifts the numbers away from
# the rehearsed narrative (5 vendors / ~1,100+ transactions / 90-day history).
#
# Usage: bash scripts/reset-demo-data.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Stopping and wiping the UKHONA PAY database volume..."
docker compose down -v
docker compose up -d

# Postgres restarts itself once mid-init (it applies schema.sql against a
# temporary server, then restarts as the real one) - pg_isready alone can
# report healthy during that brief window. schema.sql also seeds ~1,100 bulk
# transactions via a row-by-row PL/pgSQL loop (the 90-day financial-identity
# history), which takes noticeably longer than the handful of hand-written
# INSERTs - a plain "did the query return something" check can catch the 20
# hand-written rows before the bulk block finishes and declare success too
# early. So this polls the actual transaction COUNT and requires it to clear
# a threshold safely below the realistic minimum (~630 in the worst-case
# random draw) but far above the 20-row false-positive.
echo "Waiting for Postgres to finish initializing and loading the schema (seeding ~1,100+ rows can take a minute)..."
txn_count=0
for i in $(seq 1 40); do
  sleep 2
  raw=$(docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c "SELECT count(*) FROM transactions;" 2>/dev/null | tr -d '[:space:]') || raw=""
  if [[ "$raw" =~ ^[0-9]+$ ]]; then
    txn_count=$raw
    if [ "$txn_count" -gt 200 ]; then
      break
    fi
  fi
done

if [ "$txn_count" -le 200 ]; then
  echo "Postgres did not finish seeding in time (saw only $txn_count transactions) - check 'docker logs ukhonapay-postgres'." >&2
  exit 1
fi

# The transaction-count threshold above confirms the bulk seed finished, but
# Postgres can still be mid-restart at this exact instant (same transient
# window as before) - so this summary fetch gets its own short retry.
summary=""
for j in $(seq 1 5); do
  summary=$(docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c \
    "SELECT (SELECT count(*) FROM users) || ' users, ' || (SELECT count(*) FROM vendors) || ' vendors, ' || (SELECT count(*) FROM transactions) || ' transactions, R' || (SELECT round(sum(amount)) FROM transactions) || ' volume';" 2>/dev/null) || summary=""
  if [ -n "$summary" ]; then break; fi
  sleep 2
done

echo "Reset complete:$summary"
echo "Restart the backend (mvn spring-boot:run) if it was already running, so its connection pool picks up the fresh database."
