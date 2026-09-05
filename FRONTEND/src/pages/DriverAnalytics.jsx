import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Bar, Line } from "react-chartjs-2";
import { BarChart3, Car, FileText, Lightbulb } from "lucide-react";
import client from "../api/client";
import { CHART_COLORS } from "../charts/registerCharts";
import Card from "../components/ui/Card";
import FinancialScoreCard from "../components/ui/FinancialScoreCard";
import { SkeletonCard } from "../components/ui/Skeleton";
import VendorStatementModal from "../components/VendorStatementModal";
import { listContainer, listItem } from "../lib/motion";

function formatHour(hour) {
  if (hour === -1 || hour === null || hour === undefined) return "—";
  const period = hour >= 12 ? "PM" : "AM";
  const displayHour = hour % 12 === 0 ? 12 : hour % 12;
  return `${displayHour}:00 ${period}`;
}

export default function DriverAnalytics() {
  const [data, setData] = useState(null);
  const [vendor, setVendor] = useState(null);
  const [isStatementModalOpen, setIsStatementModalOpen] = useState(false);

  useEffect(() => {
    Promise.all([
      client.get("/analytics/vendor/me").catch(() => ({ data: { transactionCount: 0, totalEarned: 0, averageTransaction: 0, peakHourOfDay: -1, earningsByHour: [], earningsByDay: [], last10Transactions: [] } })),
      client.get("/vendors/me").catch(() => ({ data: null })),
    ]).then(([analyticsRes, vendorRes]) => {
      setData(analyticsRes.data);
      setVendor(vendorRes.data);
    });
  }, []);

  if (!data) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  const hourlyChart = {
    labels: (data.earningsByHour || []).map((p) => `${p.hour}h`),
    datasets: [
      {
        label: "Fare Earnings (R)",
        data: (data.earningsByHour || []).map((p) => Number(p.total)),
        backgroundColor: (data.earningsByHour || []).map((p) =>
          p.hour === data.peakHourOfDay ? CHART_COLORS.gold : CHART_COLORS.terracotta
        ),
        borderRadius: 4,
      },
    ],
  };

  const dailyChart = {
    labels: (data.earningsByDay || []).map((p) => new Date(p.date).toLocaleDateString("en-ZA", { day: "numeric", month: "short" })),
    datasets: [
      {
        label: "Daily Fares (R)",
        data: (data.earningsByDay || []).map((p) => Number(p.total)),
        borderColor: CHART_COLORS.bushveld,
        backgroundColor: "rgba(47, 143, 78, 0.15)",
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
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="flex items-center gap-2 font-display text-xl text-sand-900">
            <Car size={22} className="text-terracotta-600" /> Driver Insights
          </h1>
          <p className="text-xs text-sand-500">Fare statistics & driver PDF statement exporter</p>
        </div>
        <button
          onClick={() => setIsStatementModalOpen(true)}
          className="flex items-center gap-1.5 rounded-xl bg-terracotta-600 px-3 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-terracotta-700 active:scale-95 shrink-0"
        >
          <FileText size={15} /> PDF Statement
        </button>
      </div>

      {/* Financial Identity / Credit Score Card */}
      <div className="mb-5">
        <FinancialScoreCard />
      </div>

      <motion.div variants={listContainer} initial="initial" animate="animate" className="grid grid-cols-2 gap-3">
        {[
          { label: "Total fares received", value: data.transactionCount || 0 },
          { label: "Total earned", value: `R${Number(data.totalEarned || 0).toFixed(2)}` },
          { label: "Average fare", value: `R${Number(data.averageTransaction || 0).toFixed(2)}` },
          { label: "Peak commute hour", value: formatHour(data.peakHourOfDay) },
        ].map((stat) => (
          <motion.div key={stat.label} variants={listItem} className="rounded-xl border border-sand-200 bg-white p-4">
            <p className="text-xs text-sand-500">{stat.label}</p>
            <p className="text-xl font-semibold text-sand-900">{stat.value}</p>
          </motion.div>
        ))}
      </motion.div>

      <div className="mt-5 flex gap-2 rounded-xl border border-terracotta-200 bg-terracotta-50 p-4 text-sm text-terracotta-800">
        <Lightbulb size={16} className="mt-0.5 shrink-0" />
        <p>
          Most of your fare payments happen around <strong>{formatHour(data.peakHourOfDay)}</strong>. Make sure you are active at your registered taxi rank during peak commute hours.
        </p>
      </div>

      <Card className="mt-5">
        <h2 className="mb-3 text-sm font-semibold text-sand-700">Fares received by hour</h2>
        <div className="h-40">
          <Bar data={hourlyChart} options={chartOptions} />
        </div>
      </Card>

      <Card className="mt-5">
        <h2 className="mb-3 text-sm font-semibold text-sand-700">Daily earnings trend</h2>
        <div className="h-40">
          <Line data={dailyChart} options={chartOptions} />
        </div>
      </Card>

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-semibold text-sand-700">Recent fares received</h2>
        <div className="space-y-2">
          {(data.last10Transactions || []).map((t) => (
            <div key={t.reference} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3 text-sm">
              <span className="text-sand-500">{new Date(t.createdAt).toLocaleString("en-ZA")}</span>
              <span className="font-semibold text-bushveld-600">+R{Number(t.amount).toFixed(2)}</span>
            </div>
          ))}
          {(!data.last10Transactions || data.last10Transactions.length === 0) && (
            <p className="text-xs text-sand-400">No fare transactions recorded yet.</p>
          )}
        </div>
      </div>

      {/* PDF Statement Modal */}
      <VendorStatementModal
        isOpen={isStatementModalOpen}
        onClose={() => setIsStatementModalOpen(false)}
        vendor={vendor}
      />
    </div>
  );
}
