import { useEffect, useState } from "react";
import client from "../api/client";
import { useAuth } from "../context/AuthContext";

export default function VendorDashboard() {
  const { user } = useAuth();
  const [vendor, setVendor] = useState(null);
  const [qrImage, setQrImage] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
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
        setTransactions(t.data.slice(0, 5));
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="mx-auto max-w-md px-4 py-6 text-sm text-slate-400">Loading...</div>;

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="text-lg font-semibold text-slate-800">{vendor.businessName}</h1>
      <p className="mb-4 text-sm text-slate-500">{vendor.locationName} · {vendor.category} {vendor.verified ? "· Verified ✓" : "· Pending verification"}</p>

      <div className="rounded-2xl bg-blue-600 p-4 text-white shadow-sm">
        <p className="text-xs text-blue-100">Wallet balance</p>
        <p className="text-2xl font-semibold">R{Number(wallet.balance).toFixed(2)}</p>
      </div>

      <div className="mt-5 rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm">
        <p className="mb-3 text-sm font-medium text-slate-600">Your payment QR code</p>
        {qrImage && <img src={qrImage} alt="Vendor QR code" className="mx-auto h-48 w-48" />}
        <p className="mt-2 text-xs text-slate-400 break-all">{vendor.qrCode}</p>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3">
        <div className="rounded-xl border border-slate-200 bg-white p-3 text-center">
          <p className="text-xs text-slate-500">Rating</p>
          <p className="text-lg font-semibold text-amber-500">★ {Number(vendor.ratingAvg).toFixed(1)}</p>
          <p className="text-xs text-slate-400">{vendor.ratingCount} reviews</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-3 text-center">
          <p className="text-xs text-slate-500">Transactions</p>
          <p className="text-lg font-semibold text-slate-800">{transactions.length > 0 ? "View insights →" : "—"}</p>
        </div>
      </div>

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-semibold text-slate-700">Recent payments received</h2>
        <div className="space-y-2">
          {transactions.map((t) => (
            <div key={t.reference} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3">
              <div>
                <p className="text-sm font-medium text-slate-800">{t.senderName}</p>
                <p className="text-xs text-slate-400">{new Date(t.createdAt).toLocaleString("en-ZA")}</p>
              </div>
              <p className="text-sm font-semibold text-emerald-600">+R{Number(t.amount).toFixed(2)}</p>
            </div>
          ))}
          {transactions.length === 0 && <p className="text-sm text-slate-400">No payments received yet.</p>}
        </div>
      </div>
    </div>
  );
}
