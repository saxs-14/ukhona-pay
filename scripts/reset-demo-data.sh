#!/usr/bin/env bash
# Wipes the UKHONA PAY database and reseeds it from DATABASE/schema.sql.
# Run this right before your real demo/pitch - every test payment, withdrawal,
# or rating you or a judge makes during rehearsal drifts the numbers away from
# the rehearsed narrative (5 vendors / 20 transactions / ~R3,670 volume).
#
# Usage: bash scripts/reset-demo-data.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Stopping and wiping the UKHONA PAY database volume..."
docker compose down -v
docker compose up -d

# Postgres restarts itself once mid-init (it applies schema.sql against a
# temporary server, then restarts as the real one) - pg_isready alone can
# report healthy during that brief window. Retry the real summary query
# instead, since that only succeeds once the schema is fully loaded.
echo "Waiting for Postgres to finish initializing and loading the schema..."
counts=""
for i in $(seq 1 20); do
  sleep 2
  if counts=$(docker exec ukhonapay-postgres psql -U ukhonapay -d ukhonapay -t -c \
    "SELECT (SELECT count(*) FROM users) || ' users, ' || (SELECT count(*) FROM vendors) || ' vendors, ' || (SELECT count(*) FROM transactions) || ' transactions, R' || (SELECT round(sum(amount)) FROM transactions) || ' volume';" 2>/dev/null) && [ -n "$counts" ]; then
    break
  fi
  counts=""
done

if [ -z "$counts" ]; then
  echo "Postgres did not finish initializing in time - check 'docker logs ukhonapay-postgres'." >&2
  exit 1
fi

echo "Reset complete:$counts"
echo "Restart the backend (mvn spring-boot:run) if it was already running, so its connection pool picks up the fresh database."
