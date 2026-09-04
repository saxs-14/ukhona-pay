import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { KeyRound, Landmark, Wallet } from "lucide-react";
import client from "../api/client";
import Button from "../components/ui/Button";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { ease } from "../lib/motion";

export default function CashbackWallet() {
  const [wallet, setWallet] = useState(null);
  const [atms, setAtms] = useState([]);
  const [withdrawals, setWithdrawals] = useState([]);
  const [selectedAtm, setSelectedAtm] = useState("");
  const [amount, setAmount] = useState("");
  const [pending, setPending] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function refresh() {
    Promise.all([client.get("/wallet/me"), client.get("/withdrawals/atms"), client.get("/withdrawals/me")]).then(
      ([w, a, wd]) => {
        setWallet(w.data);
        setAtms(a.data);
        setWithdrawals(wd.data);
        if (a.data.length && !selectedAtm) setSelectedAtm(String(a.data[0].id));
      }
    );
  }

  useEffect(refresh, []);

  async function handleRequest(e) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const { data } = await client.post("/withdrawals", {
        atmLocationId: Number(selectedAtm),
        amount: Number(amount),
      });
      setPending(data);
      setAmount("");
      refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleComplete(id) {
    await client.post(`/withdrawals/${id}/complete`);
    setPending(null);
    refresh();
  }

  const maxAmount = Number(wallet?.cashbackBalance ?? 0);

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <Wallet size={20} className="text-gold-600" /> Cashback wallet
      </h1>
      <p className="mb-4 text-sm text-sand-500">Cashback is withdrawn as physical cash at an ABSA ATM — no bank transfers.</p>

      <div className="rounded-2xl bg-gradient-to-br from-bushveld-600 to-bushveld-700 p-4 text-white shadow-warm">
        <p className="text-xs text-bushveld-100">Available cashback</p>
        <p className="text-2xl font-semibold">
          <AnimatedNumber value={maxAmount} prefix="R" />
        </p>
      </div>

      <AnimatePresence mode="wait">
        {pending ? (
          <motion.div
            key="pending"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            transition={{ ease: ease.enter }}
            className="mt-4 rounded-xl border-2 border-terracotta-400 bg-terracotta-50 p-4"
          >
            <p className="flex items-center gap-2 text-sm font-semibold text-terracotta-800">
              <KeyRound size={16} /> Withdrawal PIN: {pending.withdrawalPin}
            </p>
            <p className="mt-1 text-sm text-terracotta-700">
              Go to <strong>{pending.atmName}</strong>, select cash withdrawal, and enter this PIN. Valid until{" "}
              {new Date(pending.expiresAt).toLocaleString("en-ZA")}.
            </p>
            <Button onClick={() => handleComplete(pending.id)} className="mt-3 w-full">
              I've withdrawn the cash at the ATM
            </Button>
          </motion.div>
        ) : (
          <motion.form
            key="form"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onSubmit={handleRequest}
            className="mt-5 space-y-3 rounded-xl border border-sand-200 bg-white p-4"
          >
            <h2 className="flex items-center gap-2 text-sm font-semibold text-sand-700">
              <Landmark size={15} className="text-terracotta-600" /> Request an ATM withdrawal
            </h2>
            <div>
              <label className="mb-1 block text-xs font-medium text-sand-500">Nearest ATM</label>
              <select
                value={selectedAtm}
                onChange={(e) => setSelectedAtm(e.target.value)}
                className="w-full rounded-lg border border-sand-300 px-3 py-2 text-sm focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
              >
                {atms.map((a) => (
                  <option key={a.id} value={a.id}>{a.name} — {a.city}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-sand-500">Amount (max R{maxAmount.toFixed(2)})</label>
              <input
                type="number"
                min="1"
                max={maxAmount}
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full rounded-lg border border-sand-300 px-3 py-2 text-sm focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                required
              />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" variant="success" disabled={maxAmount <= 0} loading={submitting} className="w-full">
              Withdraw cash
            </Button>
          </motion.form>
        )}
      </AnimatePresence>

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-semibold text-sand-700">Withdrawal history</h2>
        <div className="space-y-2">
          {withdrawals.map((w) => (
            <div key={w.id} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3 text-sm">
              <div>
                <p className="font-medium text-sand-800">{w.atmName}</p>
                <p className="text-xs text-sand-400">{new Date(w.requestedAt).toLocaleString("en-ZA")}</p>
              </div>
              <div className="text-right">
                <p className="font-semibold text-sand-800">R{Number(w.amount).toFixed(2)}</p>
                <p className={`text-xs ${w.status === "COMPLETED" ? "text-bushveld-600" : "text-gold-600"}`}>{w.status}</p>
              </div>
            </div>
          ))}
          {withdrawals.length === 0 && <p className="text-sm text-sand-400">No withdrawals yet.</p>}
        </div>
      </div>
    </div>
  );
}
