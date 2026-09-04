import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { Doughnut } from "react-chartjs-2";
import { ChevronRight, QrCode, Search, Wallet } from "lucide-react";
import client from "../api/client";
import { useAuth } from "../context/AuthContext";
import { CATEGORY_COLORS, CHART_COLORS } from "../charts/registerCharts";
import Card from "../components/ui/Card";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem, spring } from "../lib/motion";

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
      <motion.h1 initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="font-display text-xl text-sand-900">
        Sawubona, {user.name.split(" ")[0]}
      </motion.h1>
      <p className="mb-4 text-sm text-sand-500">Your Ukhona Pay overview</p>

      {loading ? (
        <div className="grid grid-cols-2 gap-3">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : (
        <motion.div variants={listContainer} initial="initial" animate="animate" className="grid grid-cols-2 gap-3">
          <motion.div variants={listItem} className="rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-4 text-white shadow-warm">
            <p className="text-xs text-terracotta-100">Wallet balance</p>
            <p className="text-2xl font-semibold">
              <AnimatedNumber value={Number(wallet?.balance ?? 0)} prefix="R" />
            </p>
          </motion.div>
          <motion.div variants={listItem} className="rounded-2xl bg-gradient-to-br from-bushveld-600 to-bushveld-700 p-4 text-white shadow-warm">
            <p className="text-xs text-bushveld-100">Cashback earned</p>
            <p className="text-2xl font-semibold">
              <AnimatedNumber value={Number(wallet?.cashbackBalance ?? 0)} prefix="R" />
            </p>
          </motion.div>
        </motion.div>
      )}

      <div className="mt-5 grid grid-cols-2 gap-3">
        <motion.div whileTap={{ scale: 0.97 }} transition={spring}>
          <Link to="/scan" className="flex items-center justify-center gap-2 rounded-xl border border-sand-200 bg-white py-3 text-sm font-medium text-sand-700 shadow-sm hover:border-terracotta-200 hover:bg-terracotta-50/50">
            <QrCode size={16} className="text-terracotta-600" /> Scan &amp; Pay
          </Link>
        </motion.div>
        <motion.div whileTap={{ scale: 0.97 }} transition={spring}>
          <Link to="/vendors" className="flex items-center justify-center gap-2 rounded-xl border border-sand-200 bg-white py-3 text-sm font-medium text-sand-700 shadow-sm hover:border-terracotta-200 hover:bg-terracotta-50/50">
            <Search size={16} className="text-terracotta-600" /> Find vendors
          </Link>
        </motion.div>
      </div>

      <motion.div whileTap={{ scale: 0.98 }} transition={spring}>
        <Link
          to="/cashback"
          className="mt-3 flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3 shadow-sm hover:border-terracotta-200 hover:bg-terracotta-50/50"
        >
          <span className="flex items-center gap-2 text-sm font-medium text-sand-700">
            <Wallet size={16} className="text-gold-600" /> Withdraw cashback at an ATM
          </span>
          <ChevronRight size={16} className="text-sand-400" />
        </Link>
      </motion.div>

      {hasSpending && (
        <Card className="mt-5">
          <h2 className="mb-3 text-sm font-semibold text-sand-700">Spending by category</h2>
          <div className="mx-auto max-w-[220px]">
            <Doughnut
              data={categoryChart}
              options={{ plugins: { legend: { position: "bottom", labels: { boxWidth: 10, font: { size: 11 } } } } }}
            />
          </div>
        </Card>
      )}

      <div className="mt-6">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-sand-700">Recent activity</h2>
          <Link to="/transactions" className="text-xs font-medium text-terracotta-700">See all</Link>
        </div>

        {loading && (
          <div className="space-y-2">
            <SkeletonCard />
            <SkeletonCard />
          </div>
        )}

        {!loading && transactions.length === 0 && <p className="text-sm text-sand-400">No transactions yet.</p>}

        <motion.div variants={listContainer} initial="initial" animate="animate" className="space-y-2">
          {transactions.map((t) => (
            <motion.div key={t.reference} variants={listItem} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3">
              <div>
                <p className="text-sm font-medium text-sand-800">{t.receiverName}</p>
                <p className="text-xs text-sand-400">{new Date(t.createdAt).toLocaleDateString("en-ZA")} · {t.vendorCategory}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-sand-800">-R{Number(t.amount).toFixed(2)}</p>
                <p className="text-xs text-bushveld-600">+R{Number(t.cashbackAmount).toFixed(2)} cashback</p>
              </div>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </div>
  );
}
