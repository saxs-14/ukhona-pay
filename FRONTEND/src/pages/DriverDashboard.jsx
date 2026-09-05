import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import {
  BadgeCheck,
  Banknote,
  Building2,
  Camera,
  Car,
  Clock,
  FileText,
  Hourglass,
  Landmark,
  PiggyBank,
  QrCode,
  ScanLine,
  ShieldAlert,
  TrendingUp,
  Users,
  Wrench,
  XCircle,
} from "lucide-react";
import client from "../api/client";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import FinancialScoreCard from "../components/ui/FinancialScoreCard";
import { SkeletonCard } from "../components/ui/Skeleton";
import ScanAndPayModal from "../components/ScanAndPayModal";
import VendorBankWithdrawModal from "../components/VendorBankWithdrawModal";
import VendorStatementModal from "../components/VendorStatementModal";
import CashSendModal from "../components/CashSendModal";
import { ease, listContainer, listItem, spring } from "../lib/motion";

const quickActions = [
  { action: "withdraw", label: "Bank Payout", icon: Landmark },
  { action: "cashsend", label: "CashSend", icon: Banknote },
  { action: "scan", label: "Scan & pay", icon: ScanLine },
  { to: "/driver/send", label: "Association", icon: Building2 },
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
  const [hasVendorProfile, setHasVendorProfile] = useState(false);
  const [isScanModalOpen, setIsScanModalOpen] = useState(false);
  const [isBankModalOpen, setIsBankModalOpen] = useState(false);
  const [isStatementModalOpen, setIsStatementModalOpen] = useState(false);
  const [isCashSendOpen, setIsCashSendOpen] = useState(false);

  const loadData = () => {
    Promise.all([
      client.get("/vendors/me").catch(() => null),
      client.get("/users/me"),
      client.get("/wallet/me"),
      client.get("/transactions/me"),
    ])
      .then(([v, u, w, t]) => {
        if (v) {
          setVendor(v.data);
          setHasVendorProfile(true);
        } else {
          const profile = u.data;
          setVendor({
            businessName: `${profile.name} ${profile.surname || ""}`.trim(),
            locationName: profile.rankName || profile.associationName || "Taxi driver",
            associationName: profile.associationName,
            status: "PENDING",
            verified: false,
            qrCode: null,
          });
        }
        setWallet(w.data);
        // "Recent payments received" should only ever show money coming in -
        // /transactions/me includes the driver's own outgoing payments too
        // (Scan & Pay, association dues), which this widget was previously
        // showing unfiltered, always with a misleading "+".
        setTransactions(t.data.filter((txn) => txn.direction === "RECEIVED").slice(0, 10));
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
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

  const isApproved = hasVendorProfile && vendor.status === "APPROVED";
  const isPending = vendor.status === "PENDING";

  return (
    <motion.div
      initial="initial"
      animate="animate"
      variants={{ animate: { transition: { staggerChildren: 0.06 } } }}
      className="mx-auto max-w-md px-4 py-6"
    >
      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }}>
        <p className="mb-1 text-xs font-medium uppercase tracking-wide text-terracotta-600">Driver Portal</p>
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
              {!hasVendorProfile
                ? "Taxi profile not linked yet"
                : isPending
                  ? "Registration under review"
                  : "Registration not approved"}
            </p>
            <p className="mt-0.5 text-xs opacity-90">
              {!hasVendorProfile
                ? "Your wallet and transaction history are available. Register or link your taxi profile to enable QR payments."
                : isPending
                ? `${vendor.associationName || "Your taxi association"} needs to verify your vehicle registration before you can receive payments.`
                : `${vendor.associationName || "Your taxi association"} did not approve this registration. Contact them to resolve it.`}
            </p>
          </div>
        </motion.div>
      )}

      {/* Driver Wallet Balance Card */}
      <motion.div
        variants={cardEnter}
        transition={{ duration: 0.3, ease: ease.enter }}
        className="rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-5 text-white shadow-warm"
      >
        <p className="text-xs text-terracotta-100">Driver Wallet Balance</p>
        <p className="mt-1 text-3xl font-bold">
          <AnimatedNumber value={Number(wallet.balance)} prefix="R" />
        </p>
        <p className="mt-2 text-xs text-terracotta-100">
          Wallet balance available for instant payments and bank cashouts
        </p>
      </motion.div>

      {/* Savings & Maintenance auto-allocation - 5% of every fare payment goes
          to each pot automatically (see WalletService.creditWithAutoAllocation) */}
      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }} className="mt-2 grid grid-cols-2 gap-2">
        <div className="rounded-xl border border-sand-200 bg-white p-3">
          <p className="flex items-center gap-1 text-xs text-sand-500">
            <PiggyBank size={12} className="text-bushveld-600" /> Savings
          </p>
          <p className="mt-0.5 text-base font-semibold text-sand-800">
            <AnimatedNumber value={Number(wallet.savingsBalance || 0)} prefix="R" />
          </p>
        </div>
        <div className="rounded-xl border border-sand-200 bg-white p-3">
          <p className="flex items-center gap-1 text-xs text-sand-500">
            <Wrench size={12} className="text-gold-600" /> Maintenance
          </p>
          <p className="mt-0.5 text-base font-semibold text-sand-800">
            <AnimatedNumber value={Number(wallet.maintenanceBalance || 0)} prefix="R" />
          </p>
        </div>
      </motion.div>
      <p className="mt-1.5 text-center text-[11px] text-sand-400">
        5% of every fare payment is set aside automatically into each pot
      </p>

      {/* Quick Actions Grid */}
      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }} className="mt-4 grid grid-cols-4 gap-1.5">
        {quickActions.map((a) => {
          const handleClick = (e) => {
            if (a.action === "scan") {
              e.preventDefault();
              setIsScanModalOpen(true);
            } else if (a.action === "cashsend") {
              e.preventDefault();
              setIsCashSendOpen(true);
            } else if (a.action === "withdraw") {
              e.preventDefault();
              setIsBankModalOpen(true);
            }
          };

          return (
            <motion.div key={a.label} whileHover={{ y: -2 }} whileTap={{ scale: 0.96 }} transition={spring}>
              {a.to ? (
                <Link
                  to={a.to}
                  className="flex flex-col items-center gap-1.5 rounded-xl border border-sand-200 bg-white px-1.5 py-3 text-center transition-colors hover:border-terracotta-300 hover:bg-terracotta-50"
                >
                  <a.icon size={18} className="text-terracotta-600" />
                  <span className="text-[11px] font-medium leading-tight text-sand-700">{a.label}</span>
                </Link>
              ) : (
                <button
                  type="button"
                  onClick={handleClick}
                  className="flex w-full flex-col items-center gap-1.5 rounded-xl border border-sand-200 bg-white px-1.5 py-3 text-center transition-colors hover:border-terracotta-300 hover:bg-terracotta-50"
                >
                  <a.icon size={18} className="text-terracotta-600" />
                  <span className="text-[11px] font-medium leading-tight text-sand-700">{a.label}</span>
                </button>
              )}
            </motion.div>
          );
        })}
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

      {/* Driver Insights & Statement Navigation Card */}
      <motion.div variants={cardEnter} transition={{ duration: 0.3, ease: ease.enter }}>
        <Link to="/driver/analytics" className="mt-3 flex items-center justify-between rounded-2xl border border-sand-200 bg-white p-4 transition hover:border-terracotta-300 hover:bg-sand-50 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-terracotta-50 text-terracotta-600">
              <TrendingUp size={20} />
            </div>
            <div>
              <h3 className="font-semibold text-sand-900">Driver Insights & Statement</h3>
              <p className="text-xs text-sand-500">View fare analytics & export PDF statements</p>
            </div>
          </div>
          <span className="text-sm font-bold text-terracotta-600">View →</span>
        </Link>
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

      {/* Scan & Pay Modal */}
      <ScanAndPayModal
        isOpen={isScanModalOpen}
        onClose={() => setIsScanModalOpen(false)}
        walletBalance={wallet?.balance || 0}
        onSuccess={loadData}
      />

      {/* Bank Payout Modal */}
      <VendorBankWithdrawModal
        isOpen={isBankModalOpen}
        onClose={() => setIsBankModalOpen(false)}
        walletBalance={wallet?.balance || 0}
        onSuccess={loadData}
      />

      {/* Account PDF Statement Modal */}
      <VendorStatementModal
        isOpen={isStatementModalOpen}
        onClose={() => setIsStatementModalOpen(false)}
        vendor={vendor}
      />

      {/* CashSend Modal */}
      <CashSendModal
        isOpen={isCashSendOpen}
        onClose={() => setIsCashSendOpen(false)}
        walletBalance={Number(wallet?.balance || 0)}
        onSuccess={loadData}
      />
    </motion.div>
  );
}

