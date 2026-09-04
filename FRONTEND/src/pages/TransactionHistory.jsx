import { useEffect, useMemo, useState } from "react";
import client from "../api/client";

export default function TransactionHistory() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("ALL");

  useEffect(() => {
    client.get("/transactions/me").then((res) => setTransactions(res.data)).finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    if (filter === "ALL") return transactions;
    return transactions.filter((t) => t.direction === filter);
  }, [transactions, filter]);

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-4 text-lg font-semibold text-slate-800">Transaction history</h1>

      <div className="mb-4 flex gap-2">
        {[
          { key: "ALL", label: "All" },
          { key: "SENT", label: "Sent" },
          { key: "RECEIVED", label: "Received" },
        ].map((f) => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`rounded-full border px-3 py-1.5 text-sm ${
              filter === f.key ? "border-blue-600 bg-blue-600 text-white" : "border-slate-300 bg-white text-slate-600"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading && <p className="text-sm text-slate-400">Loading...</p>}

      <div className="space-y-2">
        {filtered.map((t) => (
          <div key={t.reference} className="rounded-xl border border-slate-200 bg-white px-4 py-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-slate-800">
                {t.direction === "SENT" ? `To ${t.receiverName}` : `From ${t.senderName}`}
              </p>
              <p className={`text-sm font-semibold ${t.direction === "SENT" ? "text-slate-800" : "text-emerald-600"}`}>
                {t.direction === "SENT" ? "-" : "+"}R{Number(t.amount).toFixed(2)}
              </p>
            </div>
            <div className="mt-1 flex items-center justify-between text-xs text-slate-400">
              <span>{t.reference} · {t.status}</span>
              <span>{new Date(t.createdAt).toLocaleString("en-ZA")}</span>
            </div>
            {t.description && <p className="mt-1 text-xs text-slate-500">{t.description}</p>}
          </div>
        ))}
        {!loading && filtered.length === 0 && <p className="text-sm text-slate-400">No transactions in this view.</p>}
      </div>
    </div>
  );
}
