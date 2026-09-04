import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import client from "../api/client";
import StarRating from "../components/StarRating";

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
    return <div className="mx-auto max-w-md px-4 py-6 text-sm text-slate-400">Loading vendor...</div>;
  }

  if (step === "done" && result) {
    return (
      <div className="mx-auto max-w-md px-4 py-10 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-3xl">✅</div>
        <h1 className="text-lg font-semibold text-slate-800">Payment successful!</h1>
        <p className="mt-1 text-sm text-slate-500">R{Number(result.amount).toFixed(2)} sent to {result.vendorName}</p>

        <div className="mt-6 rounded-xl border border-slate-200 bg-white p-4 text-left text-sm">
          <div className="flex justify-between py-1"><span className="text-slate-500">Reference</span><span className="font-medium">{result.reference}</span></div>
          <div className="flex justify-between py-1"><span className="text-slate-500">Cashback earned</span><span className="font-medium text-emerald-600">+R{Number(result.cashbackEarned).toFixed(2)}</span></div>
          <div className="flex justify-between py-1"><span className="text-slate-500">New balance</span><span className="font-medium">R{Number(result.newWalletBalance).toFixed(2)}</span></div>
          <div className="flex justify-between py-1"><span className="text-slate-500">Total cashback</span><span className="font-medium">R{Number(result.newCashbackBalance).toFixed(2)}</span></div>
        </div>

        <div className="mt-6 rounded-xl border border-slate-200 bg-white p-4 text-left">
          {ratingSubmitted ? (
            <p className="text-sm text-emerald-600">Thanks for your feedback! ⭐</p>
          ) : (
            <form onSubmit={handleSubmitRating} className="space-y-3">
              <p className="text-sm font-medium text-slate-700">Rate {result.vendorName}</p>
              <StarRating value={ratingStars} onChange={setRatingStars} />
              <textarea
                placeholder="Leave a review (optional)"
                value={ratingReview}
                onChange={(e) => setRatingReview(e.target.value)}
                rows={2}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
              {ratingError && <p className="text-sm text-red-600">{ratingError}</p>}
              <button
                type="submit"
                disabled={ratingStars === 0 || ratingSubmitting}
                className="w-full rounded-lg bg-amber-500 py-2 text-sm font-medium text-white hover:bg-amber-600 disabled:opacity-60"
              >
                {ratingSubmitting ? "Submitting..." : "Submit rating"}
              </button>
            </form>
          )}
        </div>

        <div className="mt-6 flex gap-3">
          <button onClick={() => navigate("/dashboard")} className="flex-1 rounded-lg border border-slate-300 py-2.5 text-sm font-medium text-slate-700">Back home</button>
          <button onClick={() => navigate("/transactions")} className="flex-1 rounded-lg bg-blue-600 py-2.5 text-sm font-medium text-white">View history</button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-lg font-semibold text-slate-800">
              {vendor.businessName} {vendor.verified && <span className="text-blue-500">✓</span>}
            </h1>
            <p className="text-sm text-slate-500">{vendor.locationName} · {vendor.category}</p>
          </div>
          <div className="text-right">
            <p className="font-semibold text-amber-500">★ {Number(vendor.ratingAvg).toFixed(1)}</p>
            <p className="text-xs text-slate-400">{vendor.ratingCount} reviews</p>
          </div>
        </div>

        <form onSubmit={handlePay} className="mt-5 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-600">Amount (ZAR)</label>
            <div className="mb-2 flex gap-2">
              {QUICK_AMOUNTS.map((a) => (
                <button
                  type="button"
                  key={a}
                  onClick={() => setAmount(String(a))}
                  className={`flex-1 rounded-lg border py-2 text-sm font-medium ${
                    amount === String(a) ? "border-blue-600 bg-blue-50 text-blue-700" : "border-slate-300 text-slate-600"
                  }`}
                >
                  R{a}
                </button>
              ))}
            </div>
            <input
              type="number"
              min="1"
              step="0.01"
              placeholder="Custom amount"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          <input
            placeholder="What's this for? (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-600">Enter your PIN to confirm</label>
            <input
              type="password"
              inputMode="numeric"
              maxLength={4}
              value={pin}
              onChange={(e) => setPin(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 tracking-widest focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <button
            type="submit"
            disabled={submitting || !amount}
            className="w-full rounded-lg bg-blue-600 py-2.5 font-medium text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {submitting ? "Processing..." : `Pay ${vendor.businessName} ${amount ? `R${amount}` : ""}`}
          </button>
        </form>
      </div>

      <div className="mt-5">
        <h2 className="mb-2 text-sm font-semibold text-slate-700">Reviews</h2>
        <div className="space-y-2">
          {ratings.map((r, i) => (
            <div key={i} className="rounded-xl border border-slate-200 bg-white px-4 py-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-slate-800">{r.reviewerName}</p>
                <StarRating value={r.stars} readOnly size="text-sm" />
              </div>
              {r.review && <p className="mt-1 text-sm text-slate-500">{r.review}</p>}
              <p className="mt-1 text-xs text-slate-400">{new Date(r.createdAt).toLocaleDateString("en-ZA")}</p>
            </div>
          ))}
          {ratings.length === 0 && <p className="text-sm text-slate-400">No reviews yet — be the first!</p>}
        </div>
      </div>
    </div>
  );
}
