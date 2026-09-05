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

// Bank withdrawals and CashSend record sender === receiver (money left the
// wallet without going to another UKHONA PAY user), since the transactions
// table requires a receiver but there isn't a real one - without this, those
// entries render as "To <your own name>", which reads as if you paid
// yourself. The description ("Bank Cashout to FNB (****6789)", "CashSend
// Voucher to 082***0000 (...)") is already the real, useful label there.
export function isSelfTransaction(t) {
  return t.senderId != null && t.senderId === t.receiverId;
}

export function transactionPrimaryLabel(t) {
  if (isSelfTransaction(t)) return t.description || "Wallet activity";
  return t.direction === "SENT" ? t.receiverName : t.senderName;
}
