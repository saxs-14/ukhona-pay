import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import {
  BadgeCheck,
  Banknote,
  Building2,
  Car,
  Clock,
  Hourglass,
  Landmark,
  QrCode,
  ScanLine,
  ShieldAlert,
  Users,
  XCircle,
} from "lucide-react";
import client from "../api/client";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import FinancialScoreCard from "../components/ui/FinancialScoreCard";
import { SkeletonCard } from "../components/ui/Skeleton";
import { ease, listContainer, listItem, spring } from "../lib/motion";

const quickActions = [
  { to: "/driver/withdraw", label: "Withdraw", icon: Landmark },
  { to: "/driver/send", label: "Send to association", icon: Building2 },
  { to: "/driver/scan", label: "Scan & pay", icon: ScanLine },
];

const cardEnter = {
  initial: { opacity: 0, y: 14 },
  animate: { opacity: 1, y: 0 },
};

export default function DriverDashboard() {
  const [vendor, setVendor] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState("");

  useEffect(() => {
    Promise.all([client.get("/vendors/me"), client.get("/wallet/me"), client.get("/transactions/me")])
      .then(([v, w, t]) => {
        setVendor(v.data);
        setWallet(w.data);
        setTransactions(t.data.slice(0, 5));
      })
      .finally(() => setLoading(false));
  }, []);

  const payUrl = useMemo(() => {
    if (!vendor || !amount || Number(amount) <= 0) return null;
    return `${window.location.origin}/pay/${vendor.qrCode}?amount=${encodeURIComponent(amount)}`;
  }, [vendor, amount]);

  if (loading) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  const isApproved = vendor.status === "APPROVED";
  const isPending = vendor.status === "PENDING";
  const isRejected = vendor.status === "REJECTED";

  return (
    <motion.div
      initial="initial"
      animate="animate"
      variants={{ animate: { transition: { staggerChildren: 0.06 } } }}
      className="mx-auto max-w-md px-4 py-6"
    >
      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }}>
        <p className="mb-1 text-xs font-medium uppercase tracking-wide text-terracotta-600">Driver</p>
        <h1 className="flex items-center gap-1.5 font-display text-xl text-sand-900">
          {vendor.businessName} {vendor.verified && <BadgeCheck size={17} className="text-terracotta-600" />}
        </h1>
        <p className="mb-4 flex items-center gap-1 text-sm text-sand-500">
          <Users size={12} /> {vendor.locationName}
        </p>
      </motion.div>

      {!isApproved && (
        <motion.div
          variants={cardEnter}
          transition={{ duration: 0.35, ease: ease.enter }}
          className={`mb-4 flex items-start gap-3 rounded-2xl border p-4 text-sm shadow-sm ${
            isPending
              ? "border-gold-200 bg-gold-50 text-gold-800"
              : "border-red-200 bg-red-50 text-red-700"
          }`}
        >
          {isPending ? (
            <Hourglass size={20} className="mt-0.5 shrink-0 animate-pulse text-gold-600" />
          ) : (
            <XCircle size={20} className="mt-0.5 shrink-0 text-red-600" />
          )}
          <div>
            <p className="font-semibold">
              {isPending ? "Registration under review" : "Registration not approved"}
            </p>
            <p className="mt-0.5 text-xs opacity-90">
              {isPending
                ? `${vendor.associationName || "Your taxi association"} needs to verify your vehicle registration before you can receive payments.`
                : `${vendor.associationName || "Your taxi association"} did not approve this registration. Contact them to resolve it.`}
            </p>
          </div>
        </motion.div>
      )}

      <motion.div
        variants={cardEnter}
        transition={{ duration: 0.3, ease: ease.enter }}
        className="rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-4 text-white shadow-warm"
      >
        <p className="text-xs text-terracotta-100">Income received</p>
        <p className="text-2xl font-semibold">
          <AnimatedNumber value={Number(wallet.balance)} prefix="R" />
        </p>
        <p className="mt-1 text-xs text-terracotta-100">
          From commuters paying via their own banking app
        </p>
      </motion.div>

      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }} className="mt-3 grid grid-cols-3 gap-2">
        {quickActions.map((a) => (
          <motion.div key={a.to} whileHover={isApproved ? { y: -2 } : undefined} whileTap={isApproved ? { scale: 0.96 } : undefined} transition={spring}>
            <Link
              to={isApproved ? a.to : "#"}
              onClick={(e) => !isApproved && e.preventDefault()}
              aria-disabled={!isApproved}
              className={`flex flex-col items-center gap-1.5 rounded-xl border px-2 py-3 text-center transition-colors ${
                isApproved
                  ? "border-sand-200 bg-white hover:border-terracotta-300 hover:bg-terracotta-50"
                  : "cursor-not-allowed border-sand-100 bg-sand-50 opacity-50"
              }`}
            >
              <a.icon size={18} className="text-terracotta-600" />
              <span className="text-xs font-medium leading-tight text-sand-700">{a.label}</span>
            </Link>
          </motion.div>
        ))}
      </motion.div>

      {vendor.vehicleRegistration && (
        <motion.div
          variants={cardEnter}
          transition={{ duration: 0.3, ease: ease.enter }}
          className="mt-3 flex items-center gap-2 rounded-xl border border-sand-200 bg-white px-4 py-3 text-sm"
        >
          <Car size={16} className="text-terracotta-600" />
          <span className="text-sand-500">Vehicle</span>
          <span className="ml-auto font-medium text-sand-800">{vendor.vehicleRegistration}</span>
        </motion.div>
      )}

      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }}>
        <FinancialScoreCard />
      </motion.div>

      {isApproved && (
        <motion.div
          variants={cardEnter}
          transition={{ duration: 0.35, ease: ease.enter }}
          className="mt-5 rounded-2xl border border-sand-200 bg-white p-5 shadow-sm"
        >
          <p className="mb-3 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
            <QrCode size={15} className="text-terracotta-600" /> Get paid by QR code
          </p>
          <p className="mb-3 text-xs text-sand-500">
            Enter the amount you're charging, then let the commuter scan the code with their phone's
            camera to pay you from their own banking app.
          </p>
          <div className="relative">
            <Banknote size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
            <input
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              placeholder="Amount (R)"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full rounded-xl border border-sand-300 bg-sand-50/50 py-2.5 pl-10 pr-3 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100"
            />
          </div>

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
      )}

      {!isApproved && (
        <motion.div
          variants={cardEnter}
          transition={{ duration: 0.3, ease: ease.enter }}
          className="mt-5 flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-300 bg-sand-50 py-8 text-center"
        >
          <ShieldAlert size={24} className="text-sand-400" />
          <p className="text-sm text-sand-500">QR payments unlock once your registration is approved.</p>
        </motion.div>
      )}

      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }} className="mt-6">
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
      </motion.div>
    </motion.div>
  );
}
