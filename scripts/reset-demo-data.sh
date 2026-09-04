#!/usr/bin/env bash
# Wipes the UKHONA PAY database and reloads DATABASE/schema.sql.
# Schema.sql no longer seeds any user/vendor/transaction data - every account
# is created through real signup. This just reloads the reference data (ATM
# locations, taxi associations, taxi ranks) the signup dropdowns need.
#
# Usage: bash scripts/reset-demo-data.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Stopping and wiping the UKHONA PAY database volume..."
docker compose down -v
docker compose up -d

# Postgres restarts itself once mid-init (it applies schema.sql against a
# temporary server, then restarts as the real one) - pg_isready alone can
# report healthy during that brief window. Poll the actual reference-data
# count rather than just "the query returned something".
echo "Waiting for Postgres to finish initializing and loading the schema..."
rank_count=0
for i in $(seq 1 30); do
  sleep 2
  raw=$(docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c "SELECT count(*) FROM taxi_ranks;" 2>/dev/null | tr -d '[:space:]') || raw=""
  if [[ "$raw" =~ ^[0-9]+$ ]]; then
    rank_count=$raw
    if [ "$rank_count" -gt 0 ]; then
      break
    fi
  fi
done

if [ "$rank_count" -le 0 ]; then
  echo "Postgres did not finish loading the schema in time - check 'docker logs ukhonapay-postgres'." >&2
  exit 1
fi

summary=""
for j in $(seq 1 5); do
  summary=$(docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c \
    "SELECT (SELECT count(*) FROM taxi_associations) || ' taxi associations, ' || (SELECT count(*) FROM taxi_ranks) || ' taxi ranks, ' || (SELECT count(*) FROM atm_locations) || ' ATM locations, ' || (SELECT count(*) FROM users) || ' registered users';" 2>/dev/null) || summary=""
  if [ -n "$summary" ]; then break; fi
  sleep 2
done

echo "Reset complete:$summary"
echo "Restart the backend (mvn spring-boot:run) if it was already running, so its connection pool picks up the fresh database."
