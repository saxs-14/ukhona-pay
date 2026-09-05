export const PURCHASE_LABELS = {
  AIRTIME: (p) => `Airtime: ${p.network} (${p.recipientPhone})`,
  ELECTRICITY: (p) => `Electricity: ${p.municipality} (Meter: ${p.meterNumber})`,
  PAYAT_BILL: (p) => `Pay@: ${p.billerName} (Ref: ${p.payAtReference})`,
};

// Service purchases (airtime/electricity/Pay@ bills) live in their own table,
// not transactions - the money leaves the wallet without crediting any other
// UKHONA PAY wallet, the same way a bank withdrawal does, so it can't satisfy
// the transactions table's "receiver is a user or an association" constraint.
// Merges them into the transactions ledger as one chronological list, so any
// view built on top (dashboards, history) shows the same complete picture.
export function mergeTransactionHistory(transactions, purchases) {
  const purchasesAsEntries = purchases.map((p) => ({
    reference: p.reference,
    direction: "SENT",
    receiverName: PURCHASE_LABELS[p.type]?.(p) || p.type,
    amount: p.amount,
    status: "COMPLETED",
    description: `Voucher: ${p.voucherToken}`,
    createdAt: p.createdAt,
  }));
  return [...transactions, ...purchasesAsEntries].sort(
    (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
  );
}
