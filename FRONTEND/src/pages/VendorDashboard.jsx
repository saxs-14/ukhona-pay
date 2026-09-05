import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { BadgeCheck, Banknote, Camera, Clock, Landmark, MapPin, QrCode, TrendingUp, ArrowUpRight, ArrowDownLeft } from "lucide-react";
import client from "../api/client";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import ScanAndPayModal from "../components/ScanAndPayModal";
import VendorBankWithdrawModal from "../components/VendorBankWithdrawModal";
import CashSendModal from "../components/CashSendModal";
import { listContainer, listItem, spring } from "../lib/motion";
import { mergeTransactionHistory } from "../lib/transactionHistory";

export default function VendorDashboard() {
  const [vendor, setVendor] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState("");
  const [isScanModalOpen, setIsScanModalOpen] = useState(false);
  const [isBankModalOpen, setIsBankModalOpen] = useState(false);
  const [isCashSendOpen, setIsCashSendOpen] = useState(false);

  const loadData = () => {
    Promise.all([
      client.get("/vendors/me"),
      client.get("/wallet/me"),
      client.get("/transactions/me"),
      client.get("/services/history").then((res) => res.data).catch(() => []),
    ])
      .then(([v, w, t, purchases]) => {
        setVendor(v.data);
        setWallet(w.data);
        // Same merge as the full Transaction History page, just capped to the
        // 10 most recent, so this widget is a true subset of History rather
        // than a separately-computed (and previously incomplete/mislabeled) list.
        setTransactions(mergeTransactionHistory(t.data, purchases).slice(0, 10));
      })
      .finally(() => setLoading(false));
  };

  const payUrl = useMemo(() => {
    if (!vendor || !amount || Number(amount) <= 0) return null;
    return `${window.location.origin}/pay/${vendor.qrCode}?amount=${encodeURIComponent(amount)}`;
  }, [vendor, amount]);

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
      <div className="mt-3 grid grid-cols-3 gap-2">
        <button
          onClick={() => setIsScanModalOpen(true)}
          className="flex flex-col items-center justify-center gap-1 rounded-xl bg-terracotta-600 py-2.5 px-2 text-center text-xs font-semibold text-white shadow-sm transition hover:bg-terracotta-700 active:scale-98"
        >
          <Camera size={18} />
          <span>Scan & Pay</span>
        </button>
        <button
          onClick={() => setIsCashSendOpen(true)}
          className="flex flex-col items-center justify-center gap-1 rounded-xl border border-sand-200 bg-white py-2.5 px-2 text-center text-xs font-semibold text-sand-800 shadow-sm transition hover:border-terracotta-300 hover:bg-terracotta-50 active:scale-98"
        >
          <Banknote size={18} className="text-terracotta-600" />
          <span>CashSend</span>
        </button>
        <button
          onClick={() => setIsBankModalOpen(true)}
          className="flex flex-col items-center justify-center gap-1 rounded-xl bg-bushveld-600 py-2.5 px-2 text-center text-xs font-semibold text-white shadow-sm transition hover:bg-bushveld-700 active:scale-98"
        >
          <Landmark size={18} />
          <span>Bank Payout</span>
        </button>
      </div>

      {/* Get paid by QR code - enter an amount, generate a QR locked to it */}
      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 0.1 }}
        className="mt-5 rounded-2xl border border-sand-200 bg-white p-5 shadow-sm"
      >
        <p className="mb-3 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
          <QrCode size={15} className="text-terracotta-600" /> Get paid by QR code
        </p>
        <p className="mb-3 text-xs text-sand-500">
          Enter the amount you're charging, then let the customer scan the code with their phone's
          camera to pay you from their own banking app.
        </p>
        <div className="relative">
          <Banknote size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
          <input
            type="number"
            min="2.00"
            step="0.01"
            inputMode="decimal"
            placeholder="Amount (R)"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="w-full rounded-xl border border-sand-300 bg-sand-50/50 py-2.5 pl-10 pr-3 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100"
          />
        </div>
        <p className="mt-1 text-xs text-sand-400">A R1 platform fee applies.</p>

        <AnimatePresence mode="wait">
          {payUrl ? (
            <motion.div
              key="qr"
              initial={{ opacity: 0, scale: 0.92 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.92 }}
              transition={spring}
              className="mt-4 flex flex-col items-center gap-2 rounded-xl bg-sand-50 py-5"
            >
              <div className="rounded-xl bg-white p-3 shadow-sm">
                <QRCodeSVG value={payUrl} size={176} level="M" />
              </div>
              <p className="text-lg font-semibold text-terracotta-700">R{Number(amount).toFixed(2)}</p>
              <p className="text-xs text-sand-400">Ready to scan</p>
            </motion.div>
          ) : (
            <motion.p
              key="hint"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="mt-4 py-6 text-center text-xs text-sand-400"
            >
              Enter an amount above to generate a payment QR code.
            </motion.p>
          )}
        </AnimatePresence>
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
            // Same direction the full History page uses (server-computed, or
            // "SENT" for merged-in service purchases) - not re-derived here,
            // so this widget can never disagree with History for the same data.
            const isReceived = t.direction === "RECEIVED";
            return (
              <motion.div key={t.reference} variants={listItem} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3">
                <div className="flex items-center gap-3">
                  <div className={`flex h-9 w-9 items-center justify-center rounded-full ${isReceived ? "bg-bushveld-50 text-bushveld-600" : "bg-terracotta-50 text-terracotta-600"}`}>
                    {isReceived ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-sand-800">
                      {isReceived ? t.senderName : t.receiverName}
                    </p>
                    {t.description && <p className="text-xs text-sand-500">{t.description}</p>}
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

      <CashSendModal
        isOpen={isCashSendOpen}
        onClose={() => setIsCashSendOpen(false)}
        walletBalance={Number(wallet?.balance || 0)}
        onSuccess={loadData}
      />
    </div>
  );
}
