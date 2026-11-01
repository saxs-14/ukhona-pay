# Provider payment foundation

UKHONA PAY must treat an external payment as **pending until the payment provider confirms it**.

## Required flow

1. Authenticated client requests a payment intent.
2. UKHONA PAY creates a unique internal reference and idempotency key.
3. The provider receives the payment request.
4. The client may show a pending state, but must never credit a wallet from the client response.
5. Provider webhook is received by the backend.
6. Backend verifies the provider signature.
7. Backend records the provider event using its unique event ID.
8. Backend validates provider reference, amount, currency and internal reference.
9. Only then does a database transaction mark the payment completed and post the corresponding ledger/wallet movement.
10. Duplicate webhook delivery must be harmless.

## Database objects

- `payment_intents`: the authoritative lifecycle of an external payment request.
- `payment_webhook_events`: durable webhook receipt/idempotency record.

The next implementation step is the provider adapter and signed webhook handler. Provider credentials must remain deployment secrets and must never be committed to Git.
