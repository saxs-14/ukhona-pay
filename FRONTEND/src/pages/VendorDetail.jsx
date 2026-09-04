import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { BadgeCheck, MapPin, Star } from "lucide-react";
import client from "../api/client";
import StarRating from "../components/StarRating";
import Button from "../components/ui/Button";
import { ease, spring } from "../lib/motion";

const QUICK_AMOUNTS = [50, 100, 200];

export default function VendorDetail() {
  const { qrCode } = useParams();
  const navigate = useNavigate();
  const [vendor, setVendor] = useState(null);
  const [ratings, setRatings] = useState([]);
  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");
  const [description, setDescription] = useState("");
  const [step, setStep] = useState("select"); // select -> done
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [ratingStars, setRatingStars] = useState(0);
  const [ratingReview, setRatingReview] = useState("");
  const [ratingSubmitted, setRatingSubmitted] = useState(false);
  const [ratingError, setRatingError] = useState("");
  const [ratingSubmitting, setRatingSubmitting] = useState(false);

  useEffect(() => {
    client.get(`/vendors/qr/${qrCode}`).then((res) => {
      setVendor(res.data);
      client.get(`/ratings/vendor/${res.data.vendorId}`).then((r) => setRatings(r.data));
    });
  }, [qrCode]);

  async function handlePay(e) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const { data } = await client.post("/payments/pay", {
        vendorQrCode: qrCode,
        amount: Number(amount),
        pin,
        description: description || `Payment to ${vendor.businessName}`,
      });
      setResult(data);
      setStep("done");
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSubmitRating(e) {
    e.preventDefault();
    setRatingError("");
    setRatingSubmitting(true);
    try {
      await client.post("/ratings", {
        vendorId: result.vendorId,
        transactionId: result.transactionId,
        stars: ratingStars,
        review: ratingReview || null,
      });
      setRatingSubmitted(true);
    } catch (err) {
      setRatingError(err.message);
    } finally {
      setRatingSubmitting(false);
    }
  }

  if (!vendor) {
    return <div className="mx-auto max-w-md px-4 py-6 text-sm text-sand-400">Loading vendor...</div>;
  }

  if (step === "done" && result) {
    return (
      <div className="mx-auto max-w-md px-4 py-10 text-center">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: "spring", stiffness: 260, damping: 14 }}
          className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-bushveld-100"
        >
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
            <motion.path
              d="M5 13l4 4L19 7"
              stroke="#227240"
              strokeWidth={3}
              strokeLinecap="round"
              strokeLinejoin="round"
              initial={{ pathLength: 0 }}
              animate={{ pathLength: 1 }}
              transition={{ delay: 0.15, duration: 0.35, ease: "easeOut" }}
            />
          </svg>
        </motion.div>
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}>
          <h1 className="font-display text-xl text-sand-900">Payment successful!</h1>
          <p className="mt-1 text-sm text-sand-500">R{Number(result.amount).toFixed(2)} sent to {result.vendorName}</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, ease: ease.enter }}
          className="mt-6 rounded-xl border border-sand-200 bg-white p-4 text-left text-sm"
        >
          <div className="flex justify-between py-1"><span className="text-sand-500">Reference</span><span className="font-medium text-sand-800">{result.reference}</span></div>
          <div className="flex justify-between py-1"><span className="text-sand-500">Cashback earned</span><span className="font-medium text-bushveld-600">+R{Number(result.cashbackEarned).toFixed(2)}</span></div>
          <div className="flex justify-between py-1"><span className="text-sand-500">New balance</span><span className="font-medium text-sand-800">R{Number(result.newWalletBalance).toFixed(2)}</span></div>
          <div className="flex justify-between py-1"><span className="text-sand-500">Total cashback</span><span className="font-medium text-sand-800">R{Number(result.newCashbackBalance).toFixed(2)}</span></div>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className="mt-6 rounded-xl border border-sand-200 bg-white p-4 text-left">
          <AnimatePresence mode="wait">
            {ratingSubmitted ? (
              <motion.p key="thanks" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-sm text-bushveld-600">
                Thanks for your feedback!
              </motion.p>
            ) : (
              <motion.form key="form" onSubmit={handleSubmitRating} exit={{ opacity: 0 }} className="space-y-3">
                <p className="text-sm font-medium text-sand-700">Rate {result.vendorName}</p>
                <StarRating value={ratingStars} onChange={setRatingStars} />
                <textarea
                  placeholder="Leave a review (optional)"
                  value={ratingReview}
                  onChange={(e) => setRatingReview(e.target.value)}
                  rows={2}
                  className="w-full rounded-lg border border-sand-300 px-3 py-2 text-sm focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                />
                {ratingError && <p className="text-sm text-red-600">{ratingError}</p>}
                <Button type="submit" variant="success" disabled={ratingStars === 0} loading={ratingSubmitting} className="w-full">
                  Submit rating
                </Button>
              </motion.form>
            )}
          </AnimatePresence>
        </motion.div>

        <div className="mt-6 flex gap-3">
          <button onClick={() => navigate("/dashboard")} className="flex-1 rounded-xl border border-sand-300 py-2.5 text-sm font-medium text-sand-700">Back home</button>
          <Button onClick={() => navigate("/transactions")} className="flex-1">View history</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <div className="rounded-2xl border border-sand-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="flex items-center gap-1 font-display text-lg text-sand-900">
              {vendor.businessName} {vendor.verified && <BadgeCheck size={16} className="text-terracotta-600" />}
            </h1>
            <p className="flex items-center gap-1 text-sm text-sand-500">
              <MapPin size={12} /> {vendor.locationName} · {vendor.category}
            </p>
          </div>
          <div className="text-right">
            <p className="flex items-center justify-end gap-1 font-semibold text-gold-600">
              <Star size={14} fill="#E2971E" strokeWidth={0} /> {Number(vendor.ratingAvg).toFixed(1)}
            </p>
            <p className="text-xs text-sand-400">{vendor.ratingCount} reviews</p>
          </div>
        </div>

        <form onSubmit={handlePay} className="mt-5 space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-sand-700">Amount (ZAR)</label>
            <div className="mb-2 flex gap-2">
              {QUICK_AMOUNTS.map((a) => (
                <motion.button
                  type="button"
                  key={a}
                  whileTap={{ scale: 0.94 }}
                  transition={spring}
                  onClick={() => setAmount(String(a))}
                  className={`flex-1 rounded-lg border py-2 text-sm font-medium transition-colors ${
                    amount === String(a) ? "border-terracotta-600 bg-terracotta-50 text-terracotta-700" : "border-sand-300 text-sand-600"
                  }`}
                >
                  R{a}
                </motion.button>
              ))}
            </div>
            <input
              type="number"
              min="1"
              step="0.01"
              placeholder="Custom amount"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full rounded-xl border border-sand-300 px-3.5 py-2.5 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
              required
            />
          </div>

          <input
            placeholder="What's this for? (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full rounded-xl border border-sand-300 px-3.5 py-2.5 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
          />

          <div>
            <label className="mb-1.5 block text-sm font-medium text-sand-700">Enter your PIN to confirm</label>
            <input
              type="password"
              inputMode="numeric"
              maxLength={4}
              value={pin}
              onChange={(e) => setPin(e.target.value)}
              className="w-full rounded-xl border border-sand-300 px-3.5 py-2.5 tracking-[0.4em] focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
              required
            />
            <p className="mt-1.5 text-xs text-sand-400">
              This demo PIN simulates confirming from your own banking app — real commuters never create a UKHONA PAY account.
            </p>
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <Button type="submit" disabled={!amount} loading={submitting} className="w-full">
            {`Pay ${vendor.businessName} ${amount ? `R${amount}` : ""}`}
          </Button>
        </form>
      </div>

      <div className="mt-5">
        <h2 className="mb-2 text-sm font-semibold text-sand-700">Reviews</h2>
        <div className="space-y-2">
          {ratings.map((r, i) => (
            <div key={i} className="rounded-xl border border-sand-200 bg-white px-4 py-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-sand-800">{r.reviewerName}</p>
                <StarRating value={r.stars} readOnly size={14} />
              </div>
              {r.review && <p className="mt-1 text-sm text-sand-500">{r.review}</p>}
              <p className="mt-1 text-xs text-sand-400">{new Date(r.createdAt).toLocaleDateString("en-ZA")}</p>
            </div>
          ))}
          {ratings.length === 0 && <p className="text-sm text-sand-400">No reviews yet — be the first!</p>}
        </div>
      </div>
    </div>
  );
}
