import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { BadgeCheck, Camera, Clock, Landmark, MapPin, QrCode, TrendingUp, ArrowUpRight, ArrowDownLeft } from "lucide-react";
import client from "../api/client";
import { useAuth } from "../context/AuthContext";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import ScanAndPayModal from "../components/ScanAndPayModal";
import VendorBankWithdrawModal from "../components/VendorBankWithdrawModal";
import { listContainer, listItem } from "../lib/motion";

export default function VendorDashboard() {
  const { user } = useAuth();
  const [vendor, setVendor] = useState(null);
  const [qrImage, setQrImage] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isScanModalOpen, setIsScanModalOpen] = useState(false);
  const [isBankModalOpen, setIsBankModalOpen] = useState(false);

  const loadData = () => {
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
        setTransactions(t.data.slice(0, 10));
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <p className="mb-1 text-xs font-medium uppercase tracking-wide text-terracotta-600">Vendor Portal</p>
      <h1 className="flex items-center gap-1.5 font-display text-xl text-sand-900">
        {vendor.businessName} {vendor.verified && <BadgeCheck size={17} className="text-terracotta-600" />}
      </h1>
      <p className="mb-4 flex items-center gap-1 text-sm text-sand-500">
        <MapPin size={12} /> {vendor.locationName}
      </p>

      {/* Wallet Balance Card */}
      <div className="rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-5 text-white shadow-warm">
        <p className="text-xs text-terracotta-100">Vendor Wallet Balance</p>
        <p className="mt-1 text-3xl font-bold">
          <AnimatedNumber value={Number(wallet.balance)} prefix="R" />
        </p>
        <p className="mt-2 text-xs text-terracotta-100">
          Wallet balance available for instant payments and bank cashouts
        </p>
      </div>

      {/* Action Buttons Row (under the wallet card) */}
      <div className="mt-3 grid grid-cols-2 gap-3">
        <button
          onClick={() => setIsScanModalOpen(true)}
          className="flex items-center justify-center gap-2 rounded-xl bg-terracotta-600 py-3 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-terracotta-700 active:scale-98"
        >
          <Camera size={18} /> Scan & Pay
        </button>
        <button
          onClick={() => setIsBankModalOpen(true)}
          className="flex items-center justify-center gap-2 rounded-xl bg-bushveld-600 py-3 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-bushveld-700 active:scale-98"
        >
          <Landmark size={18} /> Bank Payout
        </button>
      </div>

      {/* QR Code Presentation */}
      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 0.1 }}
        className="mt-5 rounded-2xl border border-sand-200 bg-white p-5 text-center shadow-sm"
      >
        <p className="mb-3 text-sm font-medium text-sand-600">Your Receive Payment QR Code</p>
        {qrImage && <img src={qrImage} alt="Vendor QR code" className="mx-auto h-48 w-48 rounded-lg" />}
        <p className="mt-2 break-all text-xs text-sand-400">{vendor.qrCode}</p>
      </motion.div>

      {/* Business Insights Navigation */}
      <Link to="/vendor/analytics" className="mt-5 flex items-center justify-between rounded-2xl border border-sand-200 bg-white p-4 transition hover:border-terracotta-300 hover:bg-sand-50 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-terracotta-50 text-terracotta-600">
            <TrendingUp size={20} />
          </div>
          <div>
            <h3 className="font-semibold text-sand-900">Business Insights & Statement</h3>
            <p className="text-xs text-sand-500">View sales analytics & export PDF statements</p>
          </div>
        </div>
        <span className="text-sm font-bold text-terracotta-600">View →</span>
      </Link>

      {/* Transaction History */}
      <div className="mt-6">
        <h2 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
          <Clock size={14} /> Recent Wallet Activity
        </h2>
        <motion.div variants={listContainer} initial="initial" animate="animate" className="space-y-2">
          {transactions.map((t) => {
            const isReceived = t.receiverId === user.id && !t.description?.includes("Bank Cashout");
            return (
              <motion.div key={t.reference} variants={listItem} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3">
                <div className="flex items-center gap-3">
                  <div className={`flex h-9 w-9 items-center justify-center rounded-full ${isReceived ? "bg-bushveld-50 text-bushveld-600" : "bg-terracotta-50 text-terracotta-600"}`}>
                    {isReceived ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-sand-800">
                      {t.description ? t.description : (isReceived ? t.senderName : `Paid to ${t.receiverName || t.vendorName || "Vendor"}`)}
                    </p>
                    <p className="text-xs text-sand-400">{new Date(t.createdAt).toLocaleString("en-ZA")}</p>
                  </div>
                </div>
                <p className={`text-sm font-semibold ${isReceived ? "text-bushveld-600" : "text-terracotta-600"}`}>
                  {isReceived ? "+" : "-"}R{Number(t.amount).toFixed(2)}
                </p>
              </motion.div>
            );
          })}
          {transactions.length === 0 && <p className="text-sm text-sand-400">No recent wallet activity.</p>}
        </motion.div>
      </div>

      {/* Modals */}
      <ScanAndPayModal
        isOpen={isScanModalOpen}
        onClose={() => setIsScanModalOpen(false)}
        walletBalance={Number(wallet?.balance || 0)}
        onSuccess={loadData}
      />

      <VendorBankWithdrawModal
        isOpen={isBankModalOpen}
        onClose={() => setIsBankModalOpen(false)}
        walletBalance={Number(wallet?.balance || 0)}
        onSuccess={loadData}
      />
    </div>
  );
}
