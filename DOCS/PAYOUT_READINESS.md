# UKHONA PAY — Payout Readiness

## Current state

Live bank withdrawals remain disabled until a production payout provider is configured and approved.

The application now stores the provider bank identifier (bankGroupId) alongside a user's saved bank account. This is required by the current Ozow Payouts API bank-transfer request.

## Ozow production requirements

The current Ozow Payouts API uses:

- Production endpoint: https://payoutsapi.ozow.com/v1/requestpayout
- ApiKey authentication
- SiteCode header and request field
- bankGroupId
- destination account number and branch code
- SHA-512 hashCheck
- payout notification webhook
- idempotent webhook processing

Ozow states that payouts draw from merchant float and require provider approval and staging testing before production use.

## UKHONA PAY implementation rule

A withdrawal must never be marked COMPLETED merely because the provider accepted the HTTP request.

The flow must be:

1. Lock the user's wallet.
2. Validate the bank account belongs to the authenticated user.
3. Create a unique withdrawal reference.
4. Reserve/deduct the amount only inside the same database transaction as the accounting entry.
5. Submit the payout with the provider reference.
6. Keep the withdrawal PENDING until provider confirmation.
7. Verify the provider notification cryptographically.
8. Atomically transition the withdrawal state.
9. On successful payout, clear the payout-clearing ledger balance.
10. On a terminal failure or return, post a compensating ledger transaction and restore the user's available wallet balance.
11. Duplicate notifications must have no financial effect.

## Security requirements

Provider credentials, database credentials and JWT secrets must be deployment secrets. They must never be committed to Git.

Bank account numbers must not be logged. Before production launch, the stored account number must also be protected at rest using an application/database encryption design that can be rotated without losing access to existing payout destinations.

## Pilot gate

Do not enable live withdrawals until:

- provider merchant account is approved;
- payout capability is enabled by the provider;
- staging payout tests have passed;
- webhook URL is publicly reachable over HTTPS;
- webhook authenticity verification has been tested;
- bankGroupId values are populated from the provider's bank directory;
- account-number protection is enabled;
- payout success/failure/duplicate webhook tests pass;
- reconciliation between the UKHONA PAY ledger, provider payout records and merchant bank settlement is operational.
