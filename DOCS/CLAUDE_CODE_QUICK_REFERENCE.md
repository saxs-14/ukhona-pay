# UKHONA PAY — Claude Code Quick Reference
## Prioritized Tasks for Terminal Development

---

## BEFORE YOU START

You have 4 documents in /mnt/user-data/outputs/:
1. **HACKATHON_FULL_CONTEXT.md** — Complete conversation + strategy (read first for context)
2. **PLATFORM_NAME_RECOMMENDATIONS.md** — Name options (recommend: keep UKHONA PAY + tagline)
3. **UKHONA_PAY_TAXI_RANK_PROBLEM.md** — Problem statement + demo pitch
4. **UKHONA_PAY_TAXI_RANK_CHECKLIST.md** — Detailed file-by-file changes

**Start by opening HACKATHON_FULL_CONTEXT.md** to understand the full context.

---

## PLATFORM NAME DECISION

**Recommendation:** Keep **UKHONA PAY** + add tagline **"Financial Identity for Taxi Rank Traders"**

This minimizes refactoring and keeps your focus on features.

---

## PRIORITY TASKS (In Order)

### TASK 1: Update README.md (20 min)
**File:** BACKEND/../README.md

**Changes:**
```markdown
OLD HEADER:
# UKHONA PAY
South African fintech QR-payment platform for Mbombela / Nelspruit — EDHE Studentpreneurs Indaba 2026, Market Connectivity challenge

NEW HEADER:
# UKHONA PAY
## Financial Identity Platform for Taxi Drivers & Street Vendors at Taxi Ranks

South African fintech platform that helps taxi drivers and street vendors at Mbombela taxi ranks build verifiable income history through digital payments from commuters. By recording daily transactions, traders prove their cash flow to banks and unlock credit access for business growth.

ADD NEW SECTIONS:
- The Ecosystem (taxi drivers, vendors, commuters, coordinator)
- How It Works (Lucky's day example)
- The Problem (why taxi drivers need financial identity)
- The Solution (how UKHONA PAY solves it)
```

**Reference:** UKHONA_PAY_TAXI_RANK_PROBLEM.md (copy paste from there)

---

### TASK 2: Add Credit Readiness Backend Endpoint (30 min)
**File:** BACKEND/src/main/java/com/ukhona/api/controller/CreditController.java (new file)

**What to create:**
```java
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
public class CreditController {

    @GetMapping("/readiness/{vendorId}")
    public ResponseEntity<CreditReadinessDTO> getCreditReadiness(@PathVariable Long vendorId) {
        // Calculate financial score from last 90 days of transactions
        // Return: score, days recorded, consistency, total income, eligibility
    }
}
```

**Inputs needed:**
- Transaction data (already exists, just query last 90 days)
- User ID

**Outputs:**
- Financial score (0–100)
- Days recorded
- Total income
- Consistency %
- Credit eligible (bool)
- Recommended credit limit (string)

**Reference:** UKHONA_PAY_TAXI_RANK_CHECKLIST.md (full code template provided)

---

### TASK 3: Create Credit Score UI Component (30 min)
**File:** FRONTEND/src/components/CreditReadinessTile.jsx (new file)

**What to create:**
A React component that:
1. Fetches /api/credit/readiness/{vendorId}
2. Displays:
   - Financial score (72/100, for example)
   - Days recorded (92/90)
   - Consistency % (91%)
   - Total income (R12,600)
   - "ELIGIBLE FOR CREDIT" badge
   - Recommended limit "R15,000 – R20,000"

**Styling:** Terracotta/gold palette (match existing design)

**Reference:** UKHONA_PAY_TAXI_RANK_CHECKLIST.md (full code template provided)

---

### TASK 4: Add Component to Dashboard (10 min)
**File:** FRONTEND/src/components/VendorDashboardView.jsx

**Change:**
```jsx
// Add credit score tile to dashboard grid
<div className="grid grid-cols-1 md:grid-cols-3 gap-6">
  <CreditReadinessTile vendorId={vendorId} token={token} />
  {/* existing tiles */}
</div>
```

---

### TASK 5: Update Frontend Copy (45 min)
**Files to update:** All React components

**Search & Replace:**
- "Vendor" → "Taxi Driver" or "Vendor" (context-specific)
- "Employee" → "Commuter"
- "Send money" → "Record payment" or "Pay taxi fare"
- "Cashback" → "Income record"
- "Earnings" → "Daily income"
- "Payment" → "Transaction" or "Taxi payment"
- "Vendor earnings" → "Income record" or "Daily earnings"

**Key components:**
- LoginView / SignupView (add taxi rank context)
- DashboardView (change "Send money" → "Record payment")
- VendorDashboardView (change "Earnings" → "Income record")
- TransactionHistory (change headers)
- AnalyticsView (change "Vendor insights" → "Financial identity")

**Strategy:** Use Find & Replace systematically through each component

---

### TASK 6: Create Realistic Demo Data (30 min)
**File:** DATABASE/schema.sql

**What to add:**
90 days of taxi rank transactions for Lucky (taxi driver)

```sql
-- For each of 90 days, create 28 transactions (one per passenger)
-- Time: 06:30 AM to 6:30 PM
-- Amount: R10–R30 (realistic taxi fares)
-- Description: "Passenger - [destination]"
-- Pattern: Morning peak (6–8 AM), midday dip (10–12), evening peak (4–6 PM)
-- Total per day: R140–160 average
-- 90 days × 28 rides = 2,520 transactions
-- 90 days × R150 avg = R13,500 total
```

**Result:** When Lucky logs in and views analytics, judges see:
- 92 days of consistent income
- Daily average: R140
- Busiest hour: 13:00–15:00
- Financial score: 72/100
- "ELIGIBLE FOR CREDIT"

**Script to generate:** Write a small SQL generator or use raw INSERT statements

---

### TASK 7: Update DOCS/DEMO_DAY_CHECKLIST.md (20 min)
**File:** DOCS/DEMO_DAY_CHECKLIST.md

**Add sections:**
- "THE PITCH" (from UKHONA_PAY_TAXI_RANK_PROBLEM.md)
- "TESTING CHECKLIST" (verify taxi rank context on every screen)
- "KEY PHRASES" (phrases judges want to hear)
- "FALLBACK PLAN" (if tech fails, can still pitch concept)

---

### TASK 8: Create DOCS/PROBLEM_STATEMENT.md (15 min)
**File:** DOCS/PROBLEM_STATEMENT.md (new)

**Content:**
- Problem: Taxi drivers can't get loans because no income proof
- Solution: UKHONA PAY records daily transactions → 90-day history
- Challenge alignment: Challenge 3 (digitization + financial identity)
- Business model: Traders free, banks pay for data
- Scale: Mbombela Main (MVP) → 100+ ranks in SA → Africa

---

## TESTING CHECKLIST (Before Final Push)

After each task, verify:

- [ ] Platform name changed to "UKHONA PAY — Financial Identity for Taxi Rank Traders"
- [ ] README mentions taxi rank, drivers, vendors, commuters (not generic)
- [ ] Credit readiness endpoint returns realistic score (72/100 for Lucky)
- [ ] Credit score tile displays correctly on vendor dashboard
- [ ] All copy refers to "taxi driver", "vendor", "commuter", not "employee"/"payment"
- [ ] Demo data shows 90 days of transactions for Lucky
- [ ] When viewing Lucky's analytics: score 72/100, "ELIGIBLE FOR CREDIT"
- [ ] Pitch script memorized: "Lucky makes R140/day, after 90 days in UKHONA PAY he qualifies for bank loan"

---

## OPTIONAL ENHANCEMENTS (Only If Time)

If you finish the above with time left:

1. **PDF Export** — Let traders download 90-day financial report
2. **Taxi Rank Coordinator Role** — View aggregate stats for all rank traders
3. **SMS/USSD Entry** — Alternative to QR for traders who prefer SMS
4. **Export to CSV** — Traders can share data with banks
5. **Notifications** — Alert trader when they hit 30, 60, 90 days

---

## COMMAND REFERENCE (If Using Claude Code)

```bash
# Start services
docker compose up -d

# Backend development
cd BACKEND
mvn spring-boot:run

# Frontend development
cd FRONTEND
npm install
npm run dev

# Test backend endpoint
curl -H "Authorization: Bearer {token}" \
  http://localhost:8080/api/credit/readiness/1

# Verify database
psql -U ukhona_user -d ukhona_pay -c "SELECT COUNT(*) FROM transaction;"
```

---

## FILE TREE (What You're Working With)

```
ukhona-pay/
├── BACKEND/
│   ├── src/main/java/com/ukhona/
│   │   ├── api/controller/
│   │   │   ├── AuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── TransactionController.java
│   │   │   └── CreditController.java (NEW)
│   │   ├── service/
│   │   └── entity/
│   └── pom.xml
├── FRONTEND/
│   ├── src/
│   │   ├── components/
│   │   │   ├── LoginView.jsx
│   │   │   ├── DashboardView.jsx
│   │   │   ├── VendorDashboardView.jsx
│   │   │   ├── TransactionHistory.jsx
│   │   │   ├── AnalyticsView.jsx
│   │   │   └── CreditReadinessTile.jsx (NEW)
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
├── DATABASE/
│   └── schema.sql (UPDATE with 90-day data)
├── DOCS/
│   ├── DEMO_DAY_CHECKLIST.md (UPDATE)
│   ├── PROBLEM_STATEMENT.md (NEW)
│   └── (other docs)
├── docker-compose.yml
└── README.md (UPDATE)
```

---

## ESTIMATED TIMELINE

| Task | Time | Priority |
|------|------|----------|
| README.md | 20 min | HIGH |
| Credit endpoint | 30 min | HIGH |
| Credit UI tile | 30 min | HIGH |
| Add tile to dashboard | 10 min | HIGH |
| Frontend copy updates | 45 min | HIGH |
| Demo data (90 days) | 30 min | MEDIUM |
| Demo checklist | 20 min | MEDIUM |
| Problem statement doc | 15 min | MEDIUM |
| **TOTAL** | **200 min (3.3 hrs)** | — |

**Recommendation:** Finish high-priority items (3.5 hrs) first. If time permits, do medium-priority. Optional enhancements only if you're ahead of schedule.

---

## CRITICAL SUCCESS FACTORS

1. ✓ Every screen should say "taxi drivers and vendors at taxi ranks" (not generic)
2. ✓ Demo data: Lucky's 90-day transactions result in score 72/100, "ELIGIBLE FOR CREDIT"
3. ✓ Pitch is memorized and practiced: "Lucky earns R140/day, records in UKHONA PAY, gets bank loan after 90 days"
4. ✓ Credit score endpoint works and returns realistic data
5. ✓ No code breaks: Test after each major change

---

## WHEN YOU HIT PROBLEMS

**If credit endpoint fails:**
- Check /api/credit/readiness/{vendorId} with curl
- Verify demo data has 90+ days of transactions
- Ensure JWT token is valid

**If frontend won't load:**
- Check npm run dev output
- Verify /api proxy is configured
- Clear browser cache

**If demo data is unrealistic:**
- Regenerate with proper distribution (28 rides/day, R10–R30)
- Ensure 90 consecutive days (not gaps)
- Verify daily average R140 ± 20

**If you run out of time:**
- Focus on credit endpoint + tile (these are the new features judges want to see)
- Copy updates are less critical (nice to have)
- Can explain narrative verbally if needed

---

## FINAL PUSH CHECKLIST

Before demo day:
- [ ] All services running (Docker, backend, frontend)
- [ ] Preflight check passes (scripts/preflight-check.ps1)
- [ ] Credit score shows 72/100 for Lucky
- [ ] Pitch is memorized
- [ ] Demo walkthrough done 10 times (no mistakes)
- [ ] Backup plan if tech fails (can pitch concept without code)
- [ ] Team knows: this is for taxi drivers at ranks, not generic SMEs

---

**You've got this. Reference these docs, follow the checklist, and you're winning Challenge 3.**
