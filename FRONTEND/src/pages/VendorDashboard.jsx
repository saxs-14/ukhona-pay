import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { BadgeCheck, Clock, MapPin, Star } from "lucide-react";
import client from "../api/client";
import { useAuth } from "../context/AuthContext";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import FinancialScoreCard from "../components/ui/FinancialScoreCard";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem } from "../lib/motion";

export default function VendorDashboard() {
  const { user } = useAuth();
  const [vendor, setVendor] = useState(null);
  const [qrImage, setQrImage] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      client.get("/vendors/me"),
      client.get("/vendors/me/qr-image"),
      client.get("/wallet/me"),
      client.get("/transactions/me"),
    ])
      .then(([v, qr, w, t]) => {
        setVendor(v.data);
        setQrImage(qr.data.image);
        setWallet(w.data);
        setTransactions(t.data.slice(0, 5));
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  const traderLabel = vendor.category === "TAXI" ? "Driver" : "Vendor";

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <p className="mb-1 text-xs font-medium uppercase tracking-wide text-terracotta-600">{traderLabel}</p>
      <h1 className="flex items-center gap-1.5 font-display text-xl text-sand-900">
        {vendor.businessName} {vendor.verified && <BadgeCheck size={17} className="text-terracotta-600" />}
      </h1>
      <p className="mb-4 flex items-center gap-1 text-sm text-sand-500">
        <MapPin size={12} /> {vendor.locationName} · {vendor.category}
      </p>

      <div className="rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-4 text-white shadow-warm">
        <p className="text-xs text-terracotta-100">Income received</p>
        <p className="text-2xl font-semibold">
          <AnimatedNumber value={Number(wallet.balance)} prefix="R" />
        </p>
        <p className="mt-1 text-xs text-terracotta-100">
          From commuters paying via their own banking app
        </p>
      </div>

      <FinancialScoreCard />

      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 0.1 }}
        className="mt-5 rounded-2xl border border-sand-200 bg-white p-5 text-center shadow-sm"
      >
        <p className="mb-3 text-sm font-medium text-sand-600">Your payment QR code</p>
        {qrImage && <img src={qrImage} alt="Vendor QR code" className="mx-auto h-48 w-48 rounded-lg" />}
        <p className="mt-2 break-all text-xs text-sand-400">{vendor.qrCode}</p>
      </motion.div>

      <div className="mt-5 grid grid-cols-2 gap-3">
        <div className="rounded-xl border border-sand-200 bg-white p-3 text-center">
          <p className="text-xs text-sand-500">Rating</p>
          <p className="flex items-center justify-center gap-1 text-lg font-semibold text-gold-600">
            <Star size={15} fill="#E2971E" strokeWidth={0} /> {Number(vendor.ratingAvg).toFixed(1)}
          </p>
          <p className="text-xs text-sand-400">{vendor.ratingCount} reviews</p>
        </div>
        <div className="rounded-xl border border-sand-200 bg-white p-3 text-center">
          <p className="text-xs text-sand-500">Insights</p>
          <p className="text-lg font-semibold text-sand-800">
            {transactions.length > 0 ? "View →" : "—"}
          </p>
        </div>
      </div>

      <div className="mt-6">
        <h2 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
          <Clock size={14} /> Recent payments received
        </h2>
        <motion.div variants={listContainer} initial="initial" animate="animate" className="space-y-2">
          {transactions.map((t) => (
            <motion.div key={t.reference} variants={listItem} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3">
              <div>
                <p className="text-sm font-medium text-sand-800">{t.senderName}</p>
                <p className="text-xs text-sand-400">{new Date(t.createdAt).toLocaleString("en-ZA")}</p>
              </div>
              <p className="text-sm font-semibold text-bushveld-600">+R{Number(t.amount).toFixed(2)}</p>
            </motion.div>
          ))}
          {transactions.length === 0 && <p className="text-sm text-sand-400">No payments received yet.</p>}
        </motion.div>
      </div>
    </div>
  );
}
