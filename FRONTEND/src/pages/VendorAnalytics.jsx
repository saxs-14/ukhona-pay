import { useEffect, useState } from "react";
import { Bar, Line } from "react-chartjs-2";
import client from "../api/client";
import { CHART_COLORS } from "../charts/registerCharts";

function formatHour(hour) {
  if (hour === -1 || hour === null || hour === undefined) return "—";
  const period = hour >= 12 ? "PM" : "AM";
  const displayHour = hour % 12 === 0 ? 12 : hour % 12;
  return `${displayHour}:00 ${period}`;
}

export default function VendorAnalytics() {
  const [data, setData] = useState(null);

  useEffect(() => {
    client.get("/analytics/vendor/me").then((res) => setData(res.data));
  }, []);

  if (!data) return <div className="mx-auto max-w-md px-4 py-6 text-sm text-slate-400">Loading insights...</div>;

  const hourlyChart = {
    labels: data.earningsByHour.map((p) => `${p.hour}h`),
    datasets: [
      {
        label: "Earnings (R)",
        data: data.earningsByHour.map((p) => Number(p.total)),
        backgroundColor: data.earningsByHour.map((p) =>
          p.hour === data.peakHourOfDay ? CHART_COLORS.amber : CHART_COLORS.blue
        ),
        borderRadius: 4,
      },
    ],
  };

  const dailyChart = {
    labels: data.earningsByDay.map((p) => new Date(p.date).toLocaleDateString("en-ZA", { day: "numeric", month: "short" })),
    datasets: [
      {
        label: "Earnings (R)",
        data: data.earningsByDay.map((p) => Number(p.total)),
        borderColor: CHART_COLORS.emerald,
        backgroundColor: "rgba(16, 185, 129, 0.15)",
        tension: 0.3,
        fill: true,
        pointRadius: 3,
      },
    ],
  };

  const chartOptions = {
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, ticks: { font: { size: 10 } } }, x: { ticks: { font: { size: 10 } } } },
  };

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 text-lg font-semibold text-slate-800">Your business insights</h1>
      <p className="mb-4 text-sm text-slate-500">
        Generated automatically from your UKHONA PAY transaction history — no extra data entry needed.
      </p>

      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-xs text-slate-500">Total transactions</p>
          <p className="text-xl font-semibold text-slate-800">{data.transactionCount}</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-xs text-slate-500">Total earned</p>
          <p className="text-xl font-semibold text-slate-800">R{Number(data.totalEarned).toFixed(2)}</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-xs text-slate-500">Average sale</p>
          <p className="text-xl font-semibold text-slate-800">R{Number(data.averageTransaction).toFixed(2)}</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-xs text-slate-500">Busiest hour</p>
          <p className="text-xl font-semibold text-slate-800">{formatHour(data.peakHourOfDay)}</p>
        </div>
      </div>

      <div className="mt-5 rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-800">
        💡 Tip: most of your sales happen around <strong>{formatHour(data.peakHourOfDay)}</strong>. Consider having
        extra stock or staff ready at that time.
      </div>

      <div className="mt-5 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">Earnings by hour of day</h2>
        <div className="h-40">
          <Bar data={hourlyChart} options={chartOptions} />
        </div>
      </div>

      <div className="mt-5 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">Earnings trend</h2>
        <div className="h-40">
          <Line data={dailyChart} options={chartOptions} />
        </div>
      </div>

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-semibold text-slate-700">Last 10 transactions</h2>
        <div className="space-y-2">
          {data.last10Transactions.map((t) => (
            <div key={t.reference} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm">
              <span className="text-slate-500">{new Date(t.createdAt).toLocaleString("en-ZA")}</span>
              <span className="font-semibold text-slate-800">R{Number(t.amount).toFixed(2)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
