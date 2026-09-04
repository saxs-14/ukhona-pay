import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Banknote, Building2, CheckCircle2, Lock } from "lucide-react";
import client from "../api/client";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";

const inputClass =
  "w-full rounded-xl border border-sand-300 bg-sand-50/50 px-3.5 py-2.5 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100";

function Field({ label, children }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-sand-700">{label}</label>
      {children}
    </div>
  );
}

export default function DriverSendMoney() {
  const [vendor, setVendor] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);

  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [pin, setPin] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    Promise.all([client.get("/vendors/me"), client.get("/wallet/me")])
      .then(([v, w]) => {
        setVendor(v.data);
        setWallet(w.data);
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess(null);
    setSending(true);
    try {
      const { data } = await client.post("/payments/association", {
        amount: Number(amount),
        pin,
        description: description || null,
      });
      setSuccess(data);
      setWallet((w) => ({ ...w, balance: data.newWalletBalance }));
      setAmount("");
      setDescription("");
      setPin("");
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  if (!vendor.associationId) {
    return (
      <div className="mx-auto max-w-md px-4 py-6">
        <h1 className="mb-4 flex items-center gap-2 font-display text-xl text-sand-900">
          <Building2 size={20} className="text-terracotta-600" /> Send to association
        </h1>
        <Card>
          <p className="text-sm text-sand-600">
            Your driver profile isn't linked to a taxi association yet, so there's nowhere to send this payment.
          </p>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <Building2 size={20} className="text-terracotta-600" /> Send to association
      </h1>
      <p className="mb-4 text-sm text-sand-500">Pay your taxi owner or association directly from your wallet.</p>

      <div className="rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-4 text-white shadow-warm">
        <p className="text-xs text-terracotta-100">Available balance</p>
        <p className="text-2xl font-semibold">
          <AnimatedNumber value={Number(wallet.balance)} prefix="R" />
        </p>
      </div>

      <Card className="mt-5">
        <div className="mb-4 flex items-center gap-2 text-sm text-sand-600">
          <Building2 size={14} className="text-terracotta-600" />
          Sending to <span className="font-semibold text-sand-800">{vendor.associationName}</span>
        </div>
        <form onSubmit={handleSubmit} className="space-y-3">
          <Field label="Amount (R)">
            <div className="relative">
              <Banknote size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className={`${inputClass} pl-10`}
              />
            </div>
          </Field>
          <Field label="Note (optional)">
            <input value={description} onChange={(e) => setDescription(e.target.value)} className={inputClass} />
          </Field>
          <Field label="PIN">
            <div className="relative">
              <Lock size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
              <input
                required
                type="password"
                inputMode="numeric"
                maxLength={4}
                value={pin}
                onChange={(e) => setPin(e.target.value.replace(/\D/g, ""))}
                className={`${inputClass} pl-10 tracking-[0.4em]`}
              />
            </div>
          </Field>
          {error && <p className="text-sm text-red-600">{error}</p>}
          {success && (
            <motion.p
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex items-center gap-1.5 text-sm text-bushveld-600"
            >
              <CheckCircle2 size={14} /> Sent to {success.associationName} · ref {success.reference}
            </motion.p>
          )}
          <Button type="submit" loading={sending} className="w-full">
            Send
          </Button>
        </form>
      </Card>
    </div>
  );
}
