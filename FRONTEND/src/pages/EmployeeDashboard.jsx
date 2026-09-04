import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Doughnut } from "react-chartjs-2";
import client from "../api/client";
import { useAuth } from "../context/AuthContext";
import { CATEGORY_COLORS, CHART_COLORS } from "../charts/registerCharts";

export default function EmployeeDashboard() {
  const { user } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [allTransactions, setAllTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([client.get("/wallet/me"), client.get("/transactions/me")])
      .then(([w, t]) => {
        setWallet(w.data);
        setAllTransactions(t.data);
      })
      .finally(() => setLoading(false));
  }, []);

  const transactions = allTransactions.slice(0, 5);

  const categoryChart = useMemo(() => {
    const sent = allTransactions.filter((t) => t.direction === "SENT" && t.vendorCategory);
    const totals = {};
    sent.forEach((t) => {
      totals[t.vendorCategory] = (totals[t.vendorCategory] || 0) + Number(t.amount);
    });
    const labels = Object.keys(totals);
    return {
      labels,
      datasets: [
        {
          data: labels.map((l) => totals[l]),
          backgroundColor: labels.map((l) => CATEGORY_COLORS[l] || CHART_COLORS.slate),
          borderWidth: 0,
        },
      ],
    };
  }, [allTransactions]);

  const hasSpending = categoryChart.labels.length > 0;

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="text-lg font-semibold text-slate-800">Hi, {user.name.split(" ")[0]} 👋</h1>
      <p className="mb-4 text-sm text-slate-500">Here's your UKHONA PAY overview</p>

      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-2xl bg-blue-600 p-4 text-white shadow-sm">
          <p className="text-xs text-blue-100">Wallet balance</p>
          <p className="text-2xl font-semibold">
            {loading ? "..." : `R${Number(wallet?.balance ?? 0).toFixed(2)}`}
          </p>
        </div>
        <div className="rounded-2xl bg-emerald-500 p-4 text-white shadow-sm">
          <p className="text-xs text-emerald-100">Cashback earned</p>
          <p className="text-2xl font-semibold">
            {loading ? "..." : `R${Number(wallet?.cashbackBalance ?? 0).toFixed(2)}`}
          </p>
        </div>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3">
        <Link to="/scan" className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white py-3 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50">
          📷 Scan &amp; Pay
        </Link>
        <Link to="/vendors" className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white py-3 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50">
          🔍 Find vendors
        </Link>
      </div>

      <Link
        to="/cashback"
        className="mt-3 flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm hover:bg-slate-50"
      >
        <span className="text-sm font-medium text-slate-700">💵 Withdraw cashback at an ATM</span>
        <span className="text-slate-400">→</span>
      </Link>

      {hasSpending && (
        <div className="mt-5 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">Spending by category</h2>
          <div className="mx-auto max-w-[220px]">
            <Doughnut
              data={categoryChart}
              options={{
                plugins: { legend: { position: "bottom", labels: { boxWidth: 10, font: { size: 11 } } } },
              }}
            />
          </div>
        </div>
      )}

      <div className="mt-6">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">Recent activity</h2>
          <Link to="/transactions" className="text-xs font-medium text-blue-600">See all</Link>
        </div>
        <div className="space-y-2">
          {loading && <p className="text-sm text-slate-400">Loading...</p>}
          {!loading && transactions.length === 0 && (
            <p className="text-sm text-slate-400">No transactions yet.</p>
          )}
          {transactions.map((t) => (
            <div key={t.reference} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3">
              <div>
                <p className="text-sm font-medium text-slate-800">{t.receiverName}</p>
                <p className="text-xs text-slate-400">{new Date(t.createdAt).toLocaleDateString("en-ZA")} · {t.vendorCategory}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-slate-800">-R{Number(t.amount).toFixed(2)}</p>
                <p className="text-xs text-emerald-600">+R{Number(t.cashbackAmount).toFixed(2)} cashback</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
