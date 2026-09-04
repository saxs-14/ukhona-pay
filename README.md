# UKHONA PAY

South African fintech payment & savings platform — EDHE Studentpreneurs Indaba 2026, **Market Connectivity** challenge (with a light **Data Analytics** touch via the vendor insights tile).

Connects informal vendors (taxi drivers, spaza shops, food sellers, service providers) to corporate buyers through QR-code payments. Every payment earns the payer cashback, which can **only** be withdrawn as physical cash at an ABSA ATM (mocked) — never a bank transfer.

## Stack

- **Backend**: Java 21 / Spring Boot 3.3, Spring Data JPA, Spring Security + JWT, PostgreSQL, zxing (QR generation)
- **Frontend**: React 19 + Vite, React Router, Tailwind CSS, html5-qrcode (camera scanning)
- **Database**: PostgreSQL 16 (Docker), seeded with 15 users, 5 vendors, 20 transactions matching the demo script

## Running it

### 1. Database
```
docker compose up -d
```
Starts Postgres on **localhost:5442** (not 5432 — a native Postgres service was already using that port on this machine) and loads `DATABASE/schema.sql` automatically on first run.

### 2. Backend
```
cd BACKEND
mvn spring-boot:run
```
Runs on `http://localhost:8080`. JWT-secured REST API under `/api/**`; `/api/auth/**` is open.

### 3. Frontend
```
cd FRONTEND
npm install   # first time only
npm run dev
```
Runs on `http://localhost:5173`, proxies `/api` to the backend.

## Demo logins (PIN `1234` for everyone)

| Role | Phone | Name |
|---|---|---|
| Employee | `0798765432` | Karabo Mokoena |
| Vendor (Taxi) | `0711234501` | Lucky Taxi |
| Vendor (Retail) | `0711234502` | Thandi's Spaza Shop |
| Vendor (Food) | `0711234503` | Mama Joy Kitchen |

Lucky Taxi's QR code for manual entry in the scan screen: `UKP-VENDOR-LUCKYTAXI-001`

## Before you demo — read this

**`DOCS/DEMO_DAY_CHECKLIST.md`** — startup order, every environment issue we already hit and fixed (so a fresh machine doesn't surprise you), and fallback paths if something breaks live.

Two helper scripts in `scripts/` (PowerShell, tested working; `.sh` equivalents also provided):
- `scripts/preflight-check.ps1` — verifies Docker, Postgres, backend, and frontend are all actually reachable. Run this before you go on stage.
- `scripts/reset-demo-data.ps1` — wipes and reseeds the database back to the pristine demo state (5 vendors, 20 transactions, ~R3,570 volume). Run this right before your real run-through — every rehearsal payment/withdrawal/rating drifts the numbers.

## What's implemented

- Phone + PIN auth (JWT), signup for both EMPLOYEE and VENDOR account types
- Wallet balance + cashback balance
- Vendor search/filter by category, vendor profile with rating
- QR-code payment: vendor QR → amount → PIN → instant settlement + 2.5% cashback
- Immutable transaction ledger with sent/received history
- ATM cash withdrawal flow: request → PIN generated → "complete at ATM" → status update (no bank transfer path exists)
- Vendor dashboard with live QR code image and earnings
- Vendor analytics tile (transaction count, total earned, average sale, busiest hour) — derived entirely from existing transaction data, no extra input required from the vendor
- Platform-wide analytics endpoint (`/api/analytics/platform`) for a judges-facing stats dashboard
- **Ratings UI**: after a payment, the payer can leave a 1–5 star rating + optional review for that specific transaction (duplicate ratings on the same transaction are rejected with a clean 400); vendor pages show their existing reviews, and the vendor's average/count updates live
- **Chart.js dashboards**: employee dashboard shows a spending-by-category doughnut; vendor insights page shows an earnings-by-hour bar chart (peak hour highlighted) and an earnings trend line chart; a new `/platform` overview page (📊 icon in the top nav, any logged-in user) shows category breakdown and top-vendors charts for a judges-facing demo view

## Verified end-to-end

Login → wallet → vendor search → QR payment → cashback credited → transaction appears in history for both sender and vendor → ATM withdrawal requested and completed → rating submitted against the new transaction, duplicate rating attempt correctly rejected, vendor's rating average recalculated → vendor and platform analytics (including the new chart data) all confirmed working via direct API testing, and the frontend production build compiles clean.

## Known gaps for the remaining hackathon time

- No PDF export of transaction history
- No automated tests
- No visual/UI click-through testing was done in this session (browser automation wasn't connected) — worth a manual pass through the app before the demo
