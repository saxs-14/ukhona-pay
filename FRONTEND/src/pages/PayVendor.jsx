import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { Banknote, CheckCircle2, MapPin, ShieldCheck, Smartphone } from "lucide-react";
import client from "../api/client";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import { SkeletonCard } from "../components/ui/Skeleton";
import { ease } from "../lib/motion";

const QUICK_AMOUNTS = [20, 50, 100, 200];

// Public, unauthenticated page a commuter lands on after scanning a driver or
// vendor's QR code with their own phone. There is no login here and no wallet
// on this side of the payment - this simulates confirming payment from their
// own banking app, which is the real target architecture (see
// DOCS/DEMO_DAY_CHECKLIST.md). The one thing this can't do outside a real bank
// integration is actually move money out of the commuter's real account -
// clicking "Pay" here simulates that confirmation having already happened.
export default function PayVendor() {
  const { qrCode } = useParams();
  const [vendor, setVendor] = useState(null);
  const [lookupError, setLookupError] = useState("");
  const [amount, setAmount] = useState("");
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState("");
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    client
      .get(`/vendors/qr/${encodeURIComponent(qrCode)}`)
      .then((res) => setVendor(res.data))
      .catch((err) => setLookupError(err.message));
  }, [qrCode]);

  async function handlePay(e) {
    e.preventDefault();
    setPayError("");
    setPaying(true);
    try {
      const { data } = await client.post("/payments/receive", {
        vendorQrCode: qrCode,
        amount: Number(amount),
      });
      setSuccess(data);
    } catch (err) {
      setPayError(err.message);
    } finally {
      setPaying(false);
    }
  }

  if (lookupError) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-sand-50 px-4">
        <Card className="w-full max-w-sm text-center">
          <p className="text-sm text-red-600">{lookupError}</p>
        </Card>
      </div>
    );
  }

  if (!vendor) {
    return (
      <div className="mx-auto max-w-sm space-y-3 px-4 py-10">
        <SkeletonCard />
      </div>
    );
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-sand-50 px-4 py-10">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: ease.enter }}
        className="w-full max-w-sm rounded-2xl border border-sand-200 bg-white p-6 shadow-warm-lg"
      >
        {!success ? (
          <>
            <div className="mb-5 text-center">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-terracotta-600 font-display text-xl text-white shadow-warm">
                {vendor.businessName?.[0] ?? "U"}
              </div>
              <h1 className="flex items-center justify-center gap-1.5 font-display text-lg text-sand-900">
                {vendor.businessName} {vendor.verified && <ShieldCheck size={15} className="text-terracotta-600" />}
              </h1>
              <p className="mt-0.5 flex items-center justify-center gap-1 text-xs text-sand-500">
                <MapPin size={11} /> {vendor.locationName}
              </p>
            </div>

            <div className="mb-4 flex items-start gap-2 rounded-xl bg-sand-50 p-3 text-xs text-sand-500">
              <Smartphone size={14} className="mt-0.5 shrink-0 text-terracotta-500" />
              <p>You're paying from your own banking app - no UKHONA PAY account needed. This demo confirms the payment directly.</p>
            </div>

            <form onSubmit={handlePay} className="space-y-3">
              <div className="grid grid-cols-4 gap-2">
                {QUICK_AMOUNTS.map((a) => (
                  <motion.button
                    type="button"
                    key={a}
                    whileTap={{ scale: 0.94 }}
                    onClick={() => setAmount(String(a))}
                    className={`rounded-lg border py-2 text-sm font-medium transition-colors ${
                      amount === String(a) ? "border-terracotta-600 bg-terracotta-50 text-terracotta-700" : "border-sand-300 text-sand-600"
                    }`}
                  >
                    R{a}
                  </motion.button>
                ))}
              </div>
              <div className="relative">
                <Banknote size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
                <input
                  required
                  type="number"
                  min="0.01"
                  step="0.01"
                  placeholder="Custom amount"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full rounded-xl border border-sand-300 bg-sand-50/50 py-2.5 pl-10 pr-3 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                />
              </div>
              {payError && <p className="text-sm text-red-600">{payError}</p>}
              <Button type="submit" loading={paying} disabled={!amount} className="w-full">
                Pay R{amount || "0.00"}
              </Button>
            </form>
          </>
        ) : (
          <motion.div initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} className="text-center">
            <CheckCircle2 size={36} className="mx-auto mb-3 text-bushveld-600" />
            <p className="font-semibold text-sand-800">Paid R{Number(success.amount).toFixed(2)} to {success.vendorName}</p>
            <p className="mt-1 text-xs text-sand-400">ref {success.reference}</p>
            <p className="mt-4 text-xs text-sand-400">You can close this page.</p>
          </motion.div>
        )}
      </motion.div>
    </div>
  );
}
