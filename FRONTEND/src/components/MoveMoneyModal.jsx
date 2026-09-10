import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, ArrowLeftRight, AlertCircle, CheckCircle2 } from "lucide-react";
import client from "../api/client";

const POCKETS = [
  { value: "BALANCE", label: "Wallet Balance" },
  { value: "SAVINGS", label: "Savings" },
  { value: "MAINTENANCE", label: "Maintenance" },
];

function pocketAmount(wallet, pocket) {
  if (pocket === "BALANCE") return Number(wallet?.balance || 0);
  if (pocket === "SAVINGS") return Number(wallet?.savingsBalance || 0);
  return Number(wallet?.maintenanceBalance || 0);
}

// Moves money between a driver/vendor's own pockets - e.g. pulling savings
// back into the spendable balance. Never touches another user's wallet.
export default function MoveMoneyModal({ isOpen, onClose, wallet, onSuccess }) {
  const [from, setFrom] = useState("SAVINGS");
  const [to, setTo] = useState("BALANCE");
  const [amount, setAmount] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  const handleClose = () => {
    setFrom("SAVINGS");
    setTo("BALANCE");
    setAmount("");
    setError("");
    setDone(false);
    onClose();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const numAmount = Number(amount);
    if (from === to) {
      setError("Pick two different pockets to move money between");
      return;
    }
    if (!numAmount || numAmount <= 0) {
      setError("Enter an amount greater than zero");
      return;
    }
    if (numAmount > pocketAmount(wallet, from)) {
      setError(`Not enough in ${POCKETS.find((p) => p.value === from).label}`);
      return;
    }

    setLoading(true);
    try {
      await client.post("/wallet/transfer", { from, to, amount: numAmount });
      setDone(true);
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-sand-900/60 p-4 backdrop-blur-sm">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="relative w-full max-w-sm rounded-3xl border border-sand-200 bg-white p-6 shadow-2xl"
        >
          <button
            onClick={handleClose}
            className="absolute right-4 top-4 rounded-full p-2 text-sand-400 hover:bg-sand-100 hover:text-sand-700"
          >
            <X size={20} />
          </button>

          {!done ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="text-center">
                <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-2xl bg-terracotta-50 text-terracotta-600">
                  <ArrowLeftRight size={22} />
                </div>
                <h3 className="font-display text-lg text-sand-900">Move Money</h3>
                <p className="text-xs text-sand-500">Between your own pockets - nothing leaves your wallet</p>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">From</label>
                <select
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                  className="w-full rounded-xl border border-sand-300 bg-white py-2.5 px-3 text-sm text-sand-900 focus:border-terracotta-600 focus:outline-none"
                >
                  {POCKETS.map((p) => (
                    <option key={p.value} value={p.value}>
                      {p.label} (R{pocketAmount(wallet, p.value).toFixed(2)})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">To</label>
                <select
                  value={to}
                  onChange={(e) => setTo(e.target.value)}
                  className="w-full rounded-xl border border-sand-300 bg-white py-2.5 px-3 text-sm text-sand-900 focus:border-terracotta-600 focus:outline-none"
                >
                  {POCKETS.map((p) => (
                    <option key={p.value} value={p.value}>
                      {p.label} (R{pocketAmount(wallet, p.value).toFixed(2)})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Amount (ZAR)</label>
                <div className="relative flex items-center">
                  <span className="absolute left-3 text-base font-bold text-sand-400">R</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-8 pr-4 text-base font-bold text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
              </div>

              {error && (
                <div className="flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs text-rose-700">
                  <AlertCircle size={16} className="shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={handleClose}
                  className="w-1/3 rounded-xl border border-sand-300 py-3 text-sm font-semibold text-sand-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-2/3 rounded-xl bg-terracotta-600 py-3 text-sm font-semibold text-white transition hover:bg-terracotta-700 disabled:opacity-50"
                >
                  {loading ? "Moving..." : "Move money"}
                </button>
              </div>
            </form>
          ) : (
            <div className="py-2 text-center">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", stiffness: 260, damping: 20 }}
                className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-bushveld-100 text-bushveld-600"
              >
                <CheckCircle2 size={36} />
              </motion.div>
              <h3 className="font-display text-lg text-sand-900">Done</h3>
              <p className="mb-4 text-xs text-sand-500">
                R{Number(amount).toFixed(2)} moved from {POCKETS.find((p) => p.value === from).label} to{" "}
                {POCKETS.find((p) => p.value === to).label}.
              </p>
              <button
                type="button"
                onClick={handleClose}
                className="w-full rounded-xl bg-terracotta-600 py-2.5 text-sm font-semibold text-white transition hover:bg-terracotta-700"
              >
                Done
              </button>
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
