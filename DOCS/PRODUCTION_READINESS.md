# UKHONA PAY - Production Readiness

This repository is being moved from a hackathon/demo wallet into a real taxi-association pilot.

## Important distinction

The demo application contains a simulated wallet ledger. A real deployment must never treat a browser request as proof that money moved through a bank.

The production architecture therefore follows this rule:

**Bank/payment-provider confirmation -> verified webhook -> immutable UKHONA PAY ledger update -> user balance**

Never:

**Browser -> "payment successful" -> balance credit**

## Current hardening completed

- New accounts no longer receive fabricated R1,500 starting money.
- The unauthenticated `POST /api/payments/receive` endpoint was removed.
- Live runtime defaults to `UKHONA_PAY_MODE=live`.
- JWT secrets have no production fallback value.
- A separate `dev` profile is available for local demo development.
- Production deployment explicitly activates the `prod` profile.
- CI now runs frontend lint/build and backend tests.
- CORS no longer allows arbitrary private LAN origins in the deployed application.
- Production deployment does not expose demo payment injection.

## What remains before a real association handles real money

### 1. Payment provider onboarding

Choose and contract with a South African payment provider that supports the required pay-in and payout flows. Ozow currently documents Pay by Bank, QR payments, webhooks and payouts; its payout API requires merchant approval and staging/sign-off before production. Stitch also documents South African pay-ins and bank payouts. The provider must onboard UKHONA PAY/the operating business and approve the intended use case.

### 2. Real pay-in flow

Implemented in the production-hardening branch:

1. Create a PENDING payment intent with an idempotency key.
2. Create the provider payment request and persist its provider payment reference and checkout URL.
3. Return the provider checkout URL to the authenticated payer.
4. Receive Ozow One API transaction webhooks.
5. Verify the Svix signature before reading the event.
6. Support both thin and full transaction webhook payloads; thin events are resolved through the provider transaction API.
7. Verify provider transaction ID, exact merchant reference, amount and currency before settlement.
8. Apply the double-entry ledger settlement exactly once.
9. Reconcile pending pay-ins when webhooks are delayed or unavailable.
10. Keep uncertain provider/network outcomes PENDING rather than inventing a failure.
11. Reconcile completed transactions for provider refunds and reverse the original ledger/wallet allocation exactly once.

### 3. Real payouts

The production bank-withdrawal path now uses an Ozow payout adapter and an asynchronous state machine:

1. Authenticate the user and verify the account PIN.
2. Require a provider bank identifier (bankGroupId) on the saved bank account.
3. Reserve the wallet amount in a double-entry PAYOUT_CLEARING_ZAR account.
4. Create a PENDING bank-withdrawal record with a unique internal reference.
5. Generate a unique per-payout Ozow encryption key and store it encrypted with the application payout master key.
6. Encrypt the destination account number using Ozow AES-256-CBC requirements.
7. Sign and submit the payout request to Ozow.
8. Keep the withdrawal PENDING until Ozow confirms the outcome.
9. Verify the Ozow payout verification webhook, including its access token, SHA-512 hash, payout reference and banking details, before returning the decryption key.
10. Verify notification hashes and process duplicate notifications idempotently.
11. On status 5 (completed), settle the payout clearing ledger entry.
12. On status 4, 90 or 99, restore the reserved wallet amount exactly once.
13. Keep provider timeouts or unknown outcomes pending so the system does not blindly refund a payout that may already have been accepted by the provider.

The remaining production work is provider onboarding and staging certification. The code now also has scheduled payout reconciliation that can recover an accepted payout when the initial API response was lost.

### 4. Ledger

The code now contains a double-entry ledger and dedicated system accounts for provider clearing, payout clearing and platform fee revenue. Provider pay-ins and payouts post balanced ledger entries, and provider refunds create idempotent reversal entries. Wallet projections are reconciled against the vendor ledger.

Before production, the association pilot must still start from a clean, reconciled financial database and the operating entity must define settlement-account and float accounting with the payment provider.

### 5. Compliance and operations

Before a live pilot, document:

- legal operating entity
- payment-provider agreement
- settlement account
- KYC/identity requirements
- association and driver onboarding
- POPIA/privacy notice and retention rules
- fraud/chargeback/refund procedure
- transaction limits
- support/escalation process
- reconciliation process
- audit access
- incident response
- who can approve drivers, refunds and payouts

South Africa's payment system is regulated by SARB/NPSD, and consumer protection responsibilities also involve the FSCA. UKHONA PAY should operate through appropriately authorised payment partners rather than attempting to become a bank or unlicensed payment system itself.

## Pilot target

The first association should receive a controlled pilot environment with:

- one association
- one or more ranks
- approved driver roster
- unique driver QR codes
- association admin accounts
- real provider-backed payments
- real provider-backed payouts
- transaction receipts
- daily reconciliation
- audit trail
- support contact
- explicit pilot limits

No feature should be labelled "real payment", "completed", or "paid out" unless the external provider has actually confirmed it.

## Local development

Use:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
cd BACKEND
mvn spring-boot:run
```

The development profile is intentionally separate from production configuration.

## Production deployment

The GitHub Actions deployment activates the `prod` profile. Required secrets/configuration must be supplied through the deployment platform, never committed to Git.

Do not put bank credentials, provider secrets, JWT secrets, database passwords, ID numbers, PINs or webhook secrets into GitHub source files.

## Current status

**Codebase:** substantially hardened for the transition.

**Real-money readiness:** not complete until a payment provider is onboarded and the pay-in/payout/webhook/ledger flow is implemented and tested in provider staging.

That distinction is intentional: a taxi association should never be given a system that merely looks like it moves money.
