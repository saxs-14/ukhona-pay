import { useEffect, useState } from "react";
import client from "../api/client";

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
      <h1 className="mb-1 text-lg font-semibold text-slate-800">Cashback wallet</h1>
      <p className="mb-4 text-sm text-slate-500">Cashback is withdrawn as physical cash at an ABSA ATM — no bank transfers.</p>

      <div className="rounded-2xl bg-emerald-500 p-4 text-white shadow-sm">
        <p className="text-xs text-emerald-100">Available cashback</p>
        <p className="text-2xl font-semibold">R{maxAmount.toFixed(2)}</p>
      </div>

      {pending && (
        <div className="mt-4 rounded-xl border-2 border-blue-500 bg-blue-50 p-4">
          <p className="text-sm font-semibold text-blue-800">Withdrawal PIN: {pending.withdrawalPin}</p>
          <p className="mt-1 text-sm text-blue-700">
            Go to <strong>{pending.atmName}</strong>, select cash withdrawal, and enter this PIN. Valid until{" "}
            {new Date(pending.expiresAt).toLocaleString("en-ZA")}.
          </p>
          <button
            onClick={() => handleComplete(pending.id)}
            className="mt-3 w-full rounded-lg bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            I've withdrawn the cash at the ATM
          </button>
        </div>
      )}

      {!pending && (
        <form onSubmit={handleRequest} className="mt-5 space-y-3 rounded-xl border border-slate-200 bg-white p-4">
          <h2 className="text-sm font-semibold text-slate-700">Request an ATM withdrawal</h2>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Nearest ATM</label>
            <select
              value={selectedAtm}
              onChange={(e) => setSelectedAtm(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              {atms.map((a) => (
                <option key={a.id} value={a.id}>{a.name} — {a.city}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Amount (max R{maxAmount.toFixed(2)})</label>
            <input
              type="number"
              min="1"
              max={maxAmount}
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={submitting || maxAmount <= 0}
            className="w-full rounded-lg bg-emerald-600 py-2.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
          >
            {submitting ? "Requesting..." : "Withdraw cash"}
          </button>
        </form>
      )}

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-semibold text-slate-700">Withdrawal history</h2>
        <div className="space-y-2">
          {withdrawals.map((w) => (
            <div key={w.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm">
              <div>
                <p className="font-medium text-slate-800">{w.atmName}</p>
                <p className="text-xs text-slate-400">{new Date(w.requestedAt).toLocaleString("en-ZA")}</p>
              </div>
              <div className="text-right">
                <p className="font-semibold text-slate-800">R{Number(w.amount).toFixed(2)}</p>
                <p className={`text-xs ${w.status === "COMPLETED" ? "text-emerald-600" : "text-amber-500"}`}>{w.status}</p>
              </div>
            </div>
          ))}
          {withdrawals.length === 0 && <p className="text-sm text-slate-400">No withdrawals yet.</p>}
        </div>
      </div>
    </div>
  );
}
