import { useEffect, useState } from "react";
import { Bar, Doughnut } from "react-chartjs-2";
import client from "../api/client";
import { CATEGORY_COLORS, CHART_COLORS } from "../charts/registerCharts";

export default function PlatformDashboard() {
  const [data, setData] = useState(null);

  useEffect(() => {
    client.get("/analytics/platform").then((res) => setData(res.data));
  }, []);

  if (!data) return <div className="mx-auto max-w-md px-4 py-6 text-sm text-slate-400">Loading platform stats...</div>;

  const categoryLabels = Object.keys(data.categoryBreakdown);
  const categoryChart = {
    labels: categoryLabels,
    datasets: [
      {
        data: categoryLabels.map((l) => data.categoryBreakdown[l]),
        backgroundColor: categoryLabels.map((l) => CATEGORY_COLORS[l] || CHART_COLORS.slate),
        borderWidth: 0,
      },
    ],
  };

  const topVendorsChart = {
    labels: data.topVendors.map((v) => v.businessName),
    datasets: [
      {
        label: "Transactions",
        data: data.topVendors.map((v) => v.transactionCount),
        backgroundColor: CHART_COLORS.blue,
        borderRadius: 4,
      },
    ],
  };

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-800">UKHONA PAY — Platform overview</h1>
      <p className="mb-4 text-sm text-slate-500">Live stats across the whole marketplace</p>

      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-2xl bg-blue-600 p-4 text-white shadow-sm">
          <p className="text-xs text-blue-100">Total transactions</p>
          <p className="text-2xl font-semibold">{data.totalTransactions}</p>
        </div>
        <div className="rounded-2xl bg-emerald-500 p-4 text-white shadow-sm">
          <p className="text-xs text-emerald-100">Transaction volume</p>
          <p className="text-2xl font-semibold">R{Number(data.totalVolume).toFixed(0)}</p>
        </div>
        <div className="rounded-2xl bg-amber-500 p-4 text-white shadow-sm">
          <p className="text-xs text-amber-100">Cashback paid out</p>
          <p className="text-2xl font-semibold">R{Number(data.totalCashback).toFixed(2)}</p>
        </div>
        <div className="rounded-2xl bg-violet-500 p-4 text-white shadow-sm">
          <p className="text-xs text-violet-100">Active vendors</p>
          <p className="text-2xl font-semibold">{data.activeVendors}</p>
        </div>
      </div>

      <div className="mt-5 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">Category breakdown</h2>
        <div className="mx-auto max-w-[220px]">
          <Doughnut data={categoryChart} options={{ plugins: { legend: { position: "bottom", labels: { boxWidth: 10, font: { size: 11 } } } } }} />
        </div>
      </div>

      <div className="mt-5 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">Top vendors by transactions</h2>
        <div className="h-48">
          <Bar
            data={topVendorsChart}
            options={{
              indexAxis: "y",
              plugins: { legend: { display: false } },
              scales: { x: { beginAtZero: true, ticks: { font: { size: 10 } } }, y: { ticks: { font: { size: 10 } } } },
            }}
          />
        </div>
      </div>
    </div>
  );
}
