import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Building2, CheckCircle2, AlertCircle, Lock, Landmark } from "lucide-react";
import client from "../api/client";

const SOUTH_AFRICAN_BANKS = [
  "ABSA Bank",
  "First National Bank (FNB)",
  "Standard Bank",
  "Capitec Bank",
  "Nedbank",
  "TymeBank",
  "Discovery Bank",
  "African Bank"
];

export default function VendorBankWithdrawModal({ isOpen, onClose, walletBalance, onSuccess }) {
  const [bankName, setBankName] = useState("ABSA Bank");
  const [accountNumber, setAccountNumber] = useState("");
  const [accountHolderName, setAccountHolderName] = useState("");
  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successData, setSuccessData] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const numAmount = Number(amount);
    if (!numAmount || numAmount <= 0) {
      setError("Please enter a valid cashout amount");
      return;
    }
    if (numAmount > walletBalance) {
      setError(`Amount exceeds your available wallet balance (R${walletBalance.toFixed(2)})`);
      return;
    }
    if (!accountNumber || accountNumber.trim().length < 6) {
      setError("Please enter a valid bank account number");
      return;
    }
    if (!accountHolderName || !accountHolderName.trim()) {
      setError("Please enter the account holder's name");
      return;
    }
    if (!pin || pin.length !== 4) {
      setError("PIN must be 4 digits");
      return;
    }

    setLoading(true);
    try {
      const res = await client.post("/withdrawals/vendor/bank", {
        bankName,
        accountNumber,
        accountHolderName,
        amount: numAmount,
        pin,
      });
      setSuccessData(res.data);
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || "Bank cashout failed. Please check your PIN and balance.");
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setBankName("ABSA Bank");
    setAccountNumber("");
    setAccountHolderName("");
    setAmount("");
    setPin("");
    setError("");
    setSuccessData(null);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-sand-900/60 p-4 backdrop-blur-sm">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="relative w-full max-w-sm rounded-3xl border border-sand-200 bg-white p-6 shadow-2xl"
        >
          <button
            onClick={handleClose}
            className="absolute right-4 top-4 rounded-full p-2 text-sand-400 hover:bg-sand-100 hover:text-sand-700"
          >
            <X size={20} />
          </button>

          {!successData ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="text-center">
                <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-2xl bg-bushveld-50 text-bushveld-600">
                  <Landmark size={24} />
                </div>
                <h3 className="font-display text-lg text-sand-900">Withdraw to Bank</h3>
                <p className="text-xs text-sand-500">Transfer vendor wallet earnings directly to your bank account</p>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Select Bank</label>
                <select
                  value={bankName}
                  onChange={(e) => setBankName(e.target.value)}
                  className="w-full rounded-xl border border-sand-300 bg-white px-3 py-2.5 text-sm text-sand-900 focus:border-terracotta-600 focus:outline-none"
                >
                  {SOUTH_AFRICAN_BANKS.map((b) => (
                    <option key={b} value={b}>
                      {b}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="mb-1 block text-xs font-medium text-sand-600">Account Number</label>
                  <input
                    type="text"
                    placeholder="1001234567"
                    value={accountNumber}
                    onChange={(e) => setAccountNumber(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white px-3 py-2 text-sm text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-sand-600">Account Holder</label>
                  <input
                    type="text"
                    placeholder="Full Name"
                    value={accountHolderName}
                    onChange={(e) => setAccountHolderName(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white px-3 py-2 text-sm text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
              </div>

              <div className="rounded-2xl border border-sand-200 bg-sand-50 p-3.5">
                <label className="mb-1 block text-xs font-medium text-sand-600">Withdrawal Amount (ZAR)</label>
                <div className="relative flex items-center">
                  <span className="absolute left-3 text-lg font-bold text-sand-400">R</span>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-8 pr-4 text-base font-semibold text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
                <p className="mt-1.5 text-xs text-sand-500">
                  Available: <span className="font-semibold text-bushveld-700">R{walletBalance?.toFixed(2)}</span>
                </p>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Enter Your 4-Digit PIN</label>
                <div className="relative flex items-center">
                  <Lock size={16} className="absolute left-3 text-sand-400" />
                  <input
                    type="password"
                    maxLength={4}
                    placeholder="••••"
                    value={pin}
                    onChange={(e) => setPin(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-9 pr-4 text-center text-base font-bold tracking-widest text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
              </div>

              {error && (
                <div className="flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs text-rose-700">
                  <AlertCircle size={16} className="shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-xl bg-bushveld-600 py-3 text-sm font-semibold text-white transition hover:bg-bushveld-700 disabled:opacity-50"
              >
                {loading ? "Processing Payout..." : `Withdraw R${amount || "0.00"} to Bank`}
              </button>
            </form>
          ) : (
            <div className="py-6 text-center">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", stiffness: 260, damping: 20 }}
                className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-bushveld-100 text-bushveld-600"
              >
                <CheckCircle2 size={40} />
              </motion.div>
              <h3 className="font-display text-xl text-sand-900">Payout Submitted!</h3>
              <p className="mt-1 text-sm text-sand-600">
                <span className="font-bold text-bushveld-700">R{Number(successData.amount).toFixed(2)}</span> transferred to{" "}
                <span className="font-semibold text-sand-900">{successData.bankName}</span> ({successData.maskedAccount})
              </p>
              <p className="mt-2 text-xs font-mono text-sand-400">Ref: {successData.reference}</p>
              <button
                onClick={handleClose}
                className="mt-6 w-full rounded-xl bg-bushveld-600 py-3 text-sm font-semibold text-white transition hover:bg-bushveld-700"
              >
                Done
              </button>
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
