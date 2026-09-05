# UKHONA PAY — Demo Day Checklist

## Before you leave for the venue

1. Confirm Docker Desktop starts and the app runs on this exact laptop (not a machine you haven't tested on).
2. Run the reset script once, right before you pack up, so the data matches your rehearsed narrative:
   ```
   powershell -ExecutionPolicy Bypass -File scripts/reset-demo-data.ps1
   ```
3. Run the preflight check to confirm everything is green:
   ```
   powershell -ExecutionPolicy Bypass -File scripts/preflight-check.ps1
   ```

## Startup order at the venue

1. Start Docker Desktop manually if it isn't already running (it does **not** auto-start on login by default, and takes ~30-60s to come up).
2. `docker compose up -d` (from the project root)
3. `cd BACKEND && mvn spring-boot:run`
4. `cd FRONTEND && npm run dev`
5. Run `scripts/preflight-check.ps1` one more time. If anything's red, fix it before you're called up to present — don't discover it on stage.

## Right before you actually present

Run `scripts/reset-demo-data.ps1` **one more time** immediately before the real run-through if you rehearsed even once. Every payment, withdrawal, or rating made during rehearsal shifts the wallet balances and transaction counts away from the numbers in your pitch narrative (5 vendors, ~1,150-1,200 transactions across a 90-day history for Lucky and a 60-day history for Thandi, ~R30,000 volume). This bit us during development — we forgot this exact thing and had to add the reset script for it. **After resetting, always restart the backend** (`mvn spring-boot:run`) so its connection pool picks up the fresh database — the script prints this reminder.

## Suggested pitch flow — financial identity, not just payments

The core demo beat isn't "scan a QR code" — it's "90 days of informal income becomes a bank-legible credit history." Walk it as:

1. Log in as **Lucky** (`0711234501` — ask a teammate for the current PIN, see note below) — a taxi driver with 80+ days of recorded income.
2. Point at the **Income received** card — commuters paid via their own banking app, no UKHONA PAY account on their side.
3. Point at the **Financial Identity** card: score **93/100**, "Credit eligible", 80/90 days recorded, ~89% consistency, ~R19,400 verified income, and a computed lending range (~R7,500–R13,000). Say the words: *this is a readiness indicator, not a guaranteed loan* — the score comes straight out of transaction history, nothing self-reported.
4. Contrast with **Thandi** (a food vendor, shorter 60-day history) to show the score adapting to a different trader profile, and optionally **Nomsa** (`0711234505`, near-zero history) to show what "not yet eligible" looks like and why (the card states the reason in plain language).
5. Close on the reframe: UKHONA PAY isn't a wallet competing with banks — it's the record-keeping layer banks don't have for informal traders today.

## Known risks we already hit — and fixed

These are documented so if you set the project up on a **different machine**, you know what to expect:

- **Lombok doesn't work on this machine's JDK (26).** We removed Lombok entirely and wrote plain getters/setters/builders by hand. If a fresh machine has an older JDK (17/21), this isn't an issue either way — the code no longer depends on Lombok at all.
- **Port 5432 was already taken by a native Windows Postgres service**, which silently intercepted the Docker container's traffic and caused confusing "password authentication failed" errors even though the password was correct. Fixed by moving the Docker Postgres container to port **5442** — `docker-compose.yml` and `BACKEND/src/main/resources/application.yml` are already updated to match. If you move to a machine where 5442 is *also* taken, change both files together.
- **PowerShell scripts silently corrupt on em-dashes/curly quotes.** Windows PowerShell 5.1 reads `.ps1` files without a BOM using the system ANSI codepage, not UTF-8 — a UTF-8 em-dash's raw bytes get misread as a stray smart-quote character, which prematurely terminates string literals elsewhere in the file with baffling "missing terminator" errors far from the real cause. Keep any new script edits to plain ASCII punctuation (use `-` not `—`).
- **`2>$null` on a native command (docker, psql, etc.) inside a PowerShell script with `$ErrorActionPreference = "Stop"` throws a terminating exception** even for expected/harmless stderr output — this broke the reset script's retry loop. Wrap native-command calls expected to fail transiently in `try/catch` with `$ErrorActionPreference = "Continue"` for that scope.
- **Postgres restarts itself once during first-time init** (it loads `schema.sql` against a temporary server, then restarts as the real one). A single `pg_isready` check can report healthy during that brief restart window. The reset script retries the *actual* summary query instead, which only succeeds once the schema is fully loaded.
- **PowerShell's array truthiness silently breaks "did this succeed" checks.** A `docker exec ... | psql` call whose output is blank still comes back as a non-empty *array of blank lines*, and `if ($output)` treats any non-empty array as truthy — so a check can report success on a blank result. Always `($output -join "`n").Trim()` before testing truthiness, not the raw captured output.
- **A stale Service Worker from a completely different project can hijack `localhost:5173`.** Service workers are scoped to the *origin* (host+port), not to whichever dev server happens to be running there — if any earlier project ever registered one on port 5173, Chrome will keep serving its cached shell instead of UKHONA PAY, even though `curl` (no service worker support) correctly reaches the real server. Symptom: the browser shows a completely unrelated app/title on `localhost:5173` while the terminal log looks fine. Fix: DevTools → Application → Service Workers → Unregister (and Clear storage), or run this in the console: `navigator.serviceWorker.getRegistrations().then(rs => rs.forEach(r => r.unregister()))`.
- **`GET /api/transactions/me` hung for ~20 seconds once we added the 90-day bulk seed data.** `TransactionService` was looking up the sender, receiver, and vendor for every transaction with a separate `findById` call — fine for the original 20 hand-written rows, but ~2,700 sequential DB round-trips for Lucky's 900+ transactions, which silently hung the vendor dashboard (skeleton loaders stuck forever, no console error). Fixed by batching those lookups with `findAllById` into in-memory maps before mapping the response — same endpoint now responds in well under a second regardless of history length.
- **Installing new npm packages while `npm run dev` is already running can corrupt the session.** Vite's dependency optimizer detects the new packages mid-session, does a partial reload, and can leave the page in a half-updated state — we saw this as React "Invalid hook call" crashes from a newly-added icon library, and separately as entire custom Tailwind color classes silently failing to generate. Symptom is easy to miss since the page *looks* like it's rendering, just wrong. Fix: stop the dev server, delete `FRONTEND/node_modules/.vite`, restart `npm run dev`, hard-reload the browser. Safest habit: install new packages with the dev server stopped.

## Fallback paths if something breaks live

- **Camera QR scan doesn't work / camera permission denied**: use the manual QR-code text entry field already built into the Scan screen. Lucky Taxi's demo code: `UKP-VENDOR-LUCKYTAXI-001`.
- **Want to test the camera scanner before going live**: do it with two browser tabs on the *same* demo laptop, both on `http://localhost:5173` — one logged in as the vendor (showing their QR code), one logged in as the employee (scanning it with the laptop's webcam pointed at the first tab). Browsers only treat `localhost` as a secure context without HTTPS, so testing from a second device over the venue Wi-Fi (a LAN IP, not localhost) will silently fail camera access — this is a browser security rule, not a bug in the app.
- **Wi-Fi at the venue is bad or absent**: this doesn't block the demo. All Maven and npm dependencies are already downloaded and cached locally (`~/.m2`, `node_modules`), so `mvn spring-boot:run` and `npm run dev` work fully offline once started. Docker's `postgres:16-alpine` image is also already pulled locally.
- **Backend or frontend crashes mid-demo**: re-run the exact startup commands above. The database is untouched by a backend/frontend restart — only `scripts/reset-demo-data.ps1` wipes it.
- **A judge wants to sign up a live account on stage**: this works end-to-end and was tested (both EMPLOYEE and VENDOR signup). New employees start with R1,000 wallet balance; new vendors start at R0 with an auto-generated QR code.

## What's intentionally not production-hardened (fine for a same-day local demo)

- Demo account PINs are unique per account and distributed to the team over a private channel, not committed to this public repo — see `README.md`.
- The JWT signing secret and database password are plaintext defaults in `application.yml`/`docker-compose.yml`, clearly labeled as demo-only. Do not reuse these values if this ever becomes a real deployment.
- CORS is opened to `localhost`, `127.0.0.1`, and common private LAN ranges (`192.168.*`, `10.*`) so judges can browse from their own devices on the venue network — this is appropriately scoped for a local-network-only demo and would need tightening for any public deployment.
