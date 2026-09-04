import { useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import client from "../api/client";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem, spring } from "../lib/motion";

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
      <h1 className="mb-4 font-display text-xl text-sand-900">Transaction history</h1>

      <div className="mb-4 flex gap-2">
        {[
          { key: "ALL", label: "All" },
          { key: "SENT", label: "Sent" },
          { key: "RECEIVED", label: "Received" },
        ].map((f) => (
          <motion.button
            key={f.key}
            whileTap={{ scale: 0.94 }}
            transition={spring}
            onClick={() => setFilter(f.key)}
            className={`rounded-full border px-3.5 py-1.5 text-sm transition-colors ${
              filter === f.key ? "border-terracotta-600 bg-terracotta-600 text-white" : "border-sand-300 bg-white text-sand-600"
            }`}
          >
            {f.label}
          </motion.button>
        ))}
      </div>

      {loading && (
        <div className="space-y-2">
          <SkeletonCard /><SkeletonCard /><SkeletonCard />
        </div>
      )}

      <motion.div variants={listContainer} initial="initial" animate="animate" className="space-y-2">
        {filtered.map((t) => (
          <motion.div key={t.reference} variants={listItem} className="rounded-xl border border-sand-200 bg-white px-4 py-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-sand-800">
                {t.direction === "SENT" ? `To ${t.receiverName}` : `From ${t.senderName}`}
              </p>
              <p className={`text-sm font-semibold ${t.direction === "SENT" ? "text-sand-800" : "text-bushveld-600"}`}>
                {t.direction === "SENT" ? "-" : "+"}R{Number(t.amount).toFixed(2)}
              </p>
            </div>
            <div className="mt-1 flex items-center justify-between text-xs text-sand-400">
              <span>{t.reference} · {t.status}</span>
              <span>{new Date(t.createdAt).toLocaleString("en-ZA")}</span>
            </div>
            {t.description && <p className="mt-1 text-xs text-sand-500">{t.description}</p>}
          </motion.div>
        ))}
        {!loading && filtered.length === 0 && <p className="text-sm text-sand-400">No transactions in this view.</p>}
      </motion.div>
    </div>
  );
}
