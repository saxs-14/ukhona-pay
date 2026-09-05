import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Bar, Doughnut } from "react-chartjs-2";
import { MapPin } from "lucide-react";
import client from "../api/client";
import { CATEGORY_COLORS, CHART_COLORS } from "../charts/registerCharts";
import Card from "../components/ui/Card";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem } from "../lib/motion";

export default function PlatformDashboard() {
  const [data, setData] = useState(null);

  useEffect(() => {
    client.get("/analytics/platform").then((res) => setData(res.data));
  }, []);

  if (!data) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard /><SkeletonCard />
      </div>
    );
  }

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
        backgroundColor: CHART_COLORS.terracotta,
        borderRadius: 4,
      },
    ],
  };

  const stats = [
    { label: "Total transactions", value: data.totalTransactions, decimals: 0, gradient: "from-terracotta-600 to-terracotta-700" },
    { label: "Transaction volume", value: Number(data.totalVolume), decimals: 0, prefix: "R", gradient: "from-bushveld-600 to-bushveld-700" },
    { label: "Cashback paid out", value: Number(data.totalCashback), decimals: 2, prefix: "R", gradient: "from-gold-500 to-gold-600" },
    { label: "Platform fee revenue", value: Number(data.totalPlatformFees), decimals: 2, prefix: "R", gradient: "from-terracotta-500 to-gold-600" },
    { label: "Active vendors", value: data.activeVendors, decimals: 0, gradient: "from-sand-700 to-sand-800" },
  ];

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 font-display text-xl text-sand-900">Ukhona Pay — Platform overview</h1>
      <p className="mb-4 flex items-center gap-1 text-sm text-sand-500">
        <MapPin size={13} /> Live stats across Mbombela &amp; Nelspruit
      </p>

      <motion.div variants={listContainer} initial="initial" animate="animate" className="grid grid-cols-2 gap-3">
        {stats.map((s) => (
          <motion.div key={s.label} variants={listItem} className={`rounded-2xl bg-gradient-to-br p-4 text-white shadow-warm ${s.gradient}`}>
            <p className="text-xs text-white/80">{s.label}</p>
            <p className="text-2xl font-semibold">
              <AnimatedNumber value={s.value} prefix={s.prefix || ""} decimals={s.decimals} />
            </p>
          </motion.div>
        ))}
      </motion.div>

      <Card className="mt-5">
        <h2 className="mb-3 text-sm font-semibold text-sand-700">Category breakdown</h2>
        <div className="mx-auto max-w-[220px]">
          <Doughnut data={categoryChart} options={{ plugins: { legend: { position: "bottom", labels: { boxWidth: 10, font: { size: 11 } } } } }} />
        </div>
      </Card>

      <Card className="mt-5">
        <h2 className="mb-3 text-sm font-semibold text-sand-700">Top vendors by transactions</h2>
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
      </Card>
    </div>
  );
}
