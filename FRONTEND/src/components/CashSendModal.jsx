import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Banknote, CheckCircle2, AlertCircle, Phone, Lock, Copy, Share2, Store, Landmark } from "lucide-react";
import client from "../api/client";

export default function CashSendModal({ isOpen, onClose, walletBalance, onSuccess }) {
  const [step, setStep] = useState("FORM"); // "FORM", "SUCCESS"
  const [recipientPhone, setRecipientPhone] = useState("");
  const [amount, setAmount] = useState("");
  const [cashSendPin, setCashSendPin] = useState("");
  const [accountPin, setAccountPin] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [response, setResponse] = useState(null);
  const [copied, setCopied] = useState(false);

  const presets = ["50", "100", "200", "500", "1000"];

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const numAmount = Number(amount);
    if (!numAmount || numAmount < 10) {
      setError("Minimum CashSend amount is R10.00");
      return;
    }
    if (numAmount > walletBalance) {
      setError(`Insufficient balance. Available: R${walletBalance?.toFixed(2)}`);
      return;
    }
    if (!recipientPhone || recipientPhone.trim().length < 10) {
      setError("Please enter a valid 10-digit South African mobile phone number");
      return;
    }
    if (!cashSendPin || cashSendPin.length !== 4) {
      setError("Please set a 4-digit CashSend withdrawal PIN for recipient ATM cashout");
      return;
    }
    if (!accountPin || accountPin.length !== 4) {
      setError("Please enter your 4-digit account security PIN");
      return;
    }

    setLoading(true);
    try {
      const res = await client.post("/withdrawals/cash-send", {
        recipientPhone: recipientPhone.trim(),
        amount: numAmount,
        cashSendPin,
        accountPin,
      });

      setResponse(res.data);
      setStep("SUCCESS");
      if (onSuccess) onSuccess();
    } catch (err) {
      const backendMsg = err.response?.data?.message || err.message;
      setError(backendMsg || "CashSend voucher creation failed. Please check your PIN and balance.");
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (!response) return;
    const text = `UKHONA PAY CashSend Voucher:\nVoucher No: ${response.voucherNumber}\nWithdrawal PIN: ${response.cashSendPin}\nAmount: R${Number(response.amount).toFixed(2)}\nRedeem at ABSA ATMs, Shoprite, Checkers, Boxer, Usave, Pick n Pay, PEP & Spar stores.`;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleClose = () => {
    setStep("FORM");
    setRecipientPhone("");
    setAmount("");
    setCashSendPin("");
    setAccountPin("");
    setError("");
    setResponse(null);
    setCopied(false);
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

          {step === "FORM" && (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="text-center">
                <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-2xl bg-terracotta-50 text-terracotta-600">
                  <Banknote size={24} />
                </div>
                <h3 className="font-display text-lg text-sand-900">CashSend / Instant Voucher</h3>
                <p className="text-xs text-sand-500">Cash out at ABSA ATMs, Shoprite, Checkers, Boxer, PEP & Spar</p>
              </div>

              {/* Recipient Phone */}
              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Recipient Mobile Number</label>
                <div className="relative flex items-center">
                  <Phone size={16} className="absolute left-3 text-sand-400" />
                  <input
                    type="tel"
                    placeholder="e.g. 082 123 4567"
                    value={recipientPhone}
                    onChange={(e) => setRecipientPhone(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2.5 pl-9 pr-4 text-sm text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
              </div>

              {/* Amount Selection */}
              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Withdrawal Amount (ZAR)</label>
                <div className="relative flex items-center mb-2">
                  <span className="absolute left-3 text-base font-bold text-sand-400">R</span>
                  <input
                    type="number"
                    step="10"
                    min="10"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-8 pr-4 text-base font-bold text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>

                <div className="grid grid-cols-5 gap-1">
                  {presets.map((val) => (
                    <button
                      key={val}
                      type="button"
                      onClick={() => setAmount(val)}
                      className={`rounded-lg border py-1 text-xs font-semibold transition ${
                        amount === val
                          ? "border-terracotta-600 bg-terracotta-50 text-terracotta-700"
                          : "border-sand-200 bg-white text-sand-700 hover:bg-sand-50"
                      }`}
                    >
                      R{val}
                    </button>
                  ))}
                </div>
                <p className="mt-1 text-[11px] text-sand-500">
                  Available Balance: <span className="font-semibold text-bushveld-700">R{walletBalance?.toFixed(2)}</span>
                </p>
              </div>

              {/* CashSend Withdrawal PIN */}
              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Set 4-Digit CashSend ATM/Store PIN</label>
                <div className="relative flex items-center">
                  <Lock size={16} className="absolute left-3 text-sand-400" />
                  <input
                    type="password"
                    maxLength={4}
                    placeholder="••••"
                    value={cashSendPin}
                    onChange={(e) => setCashSendPin(e.target.value.replace(/\D/g, ""))}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-9 pr-4 text-center text-base font-bold tracking-widest text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
                <p className="mt-0.5 text-[10px] text-sand-400">PIN recipient enters at ATM or till point to collect cash</p>
              </div>

              {/* Account Security PIN */}
              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Enter Your 4-Digit Account Security PIN</label>
                <div className="relative flex items-center">
                  <Lock size={16} className="absolute left-3 text-sand-400" />
                  <input
                    type="password"
                    maxLength={4}
                    placeholder="••••"
                    value={accountPin}
                    onChange={(e) => setAccountPin(e.target.value.replace(/\D/g, ""))}
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

              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={handleClose}
                  className="w-1/3 rounded-xl border border-sand-300 py-3 text-sm font-semibold text-sand-700 hover:bg-sand-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-2/3 rounded-xl bg-terracotta-600 py-3 text-sm font-semibold text-white transition hover:bg-terracotta-700 disabled:opacity-50"
                >
                  {loading ? "Generating..." : `Create Voucher`}
                </button>
              </div>
            </form>
          )}

          {step === "SUCCESS" && response && (
            <div className="py-2 text-center">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", stiffness: 260, damping: 20 }}
                className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-bushveld-100 text-bushveld-600"
              >
                <CheckCircle2 size={36} />
              </motion.div>

              <h3 className="font-display text-lg text-sand-900">CashSend Voucher Created!</h3>
              <p className="text-xs text-sand-500 mb-3">Voucher created for <span className="font-semibold text-sand-800">{response.maskedPhone}</span></p>

              {/* Voucher Box */}
              <div className="rounded-2xl border border-terracotta-200 bg-terracotta-50/70 p-4 text-center mb-3">
                <p className="text-[10px] font-bold uppercase tracking-wider text-terracotta-600">10-Digit CashSend Voucher Number</p>
                <p className="mt-1 font-mono text-2xl font-black text-terracotta-900 tracking-wider">
                  {response.voucherNumber}
                </p>
                <div className="mt-2 flex justify-center gap-4 border-t border-terracotta-200/60 pt-2 text-xs">
                  <div>
                    <span className="text-[10px] text-sand-500 block">Withdrawal PIN</span>
                    <span className="font-mono font-bold text-sand-900">{response.cashSendPin}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-sand-500 block">Voucher Amount</span>
                    <span className="font-bold text-bushveld-700">R{Number(response.amount).toFixed(2)}</span>
                  </div>
                </div>
              </div>

              {/* Participating Outlets Badge List */}
              <div className="mb-4 rounded-xl border border-sand-200 bg-sand-50 p-3 text-left">
                <p className="flex items-center gap-1 text-[11px] font-bold text-sand-700 mb-1.5">
                  <Store size={14} className="text-terracotta-600" /> Redeemable At Participating Cash Outlets:
                </p>
                <div className="flex flex-wrap gap-1">
                  {["ABSA ATMs", "Shoprite", "Checkers", "Usave", "Boxer", "Pick n Pay", "PEP", "Spar"].map((store) => (
                    <span key={store} className="rounded-md bg-white border border-sand-200 px-2 py-0.5 text-[10px] font-semibold text-sand-800">
                      {store}
                    </span>
                  ))}
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleCopy}
                  className="flex w-1/2 items-center justify-center gap-1.5 rounded-xl border border-sand-300 py-2.5 text-xs font-semibold text-sand-700 hover:bg-sand-50"
                >
                  <Copy size={14} /> {copied ? "Copied!" : "Copy Voucher"}
                </button>
                <button
                  type="button"
                  onClick={handleClose}
                  className="w-1/2 rounded-xl bg-terracotta-600 py-2.5 text-xs font-semibold text-white transition hover:bg-terracotta-700"
                >
                  Done
                </button>
              </div>
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
