import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import client from "../api/client";

const CATEGORIES = ["ALL", "TAXI", "FOOD", "SERVICES", "RETAIL", "OTHER"];

export default function VendorSearch() {
  const [category, setCategory] = useState("ALL");
  const [vendors, setVendors] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    const params = category === "ALL" ? {} : { category };
    client
      .get("/vendors", { params })
      .then((res) => setVendors(res.data))
      .finally(() => setLoading(false));
  }, [category]);

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-4 text-lg font-semibold text-slate-800">Find a vendor</h1>

      <div className="mb-4 flex gap-2 overflow-x-auto pb-1">
        {CATEGORIES.map((c) => (
          <button
            key={c}
            onClick={() => setCategory(c)}
            className={`whitespace-nowrap rounded-full border px-3 py-1.5 text-sm ${
              category === c
                ? "border-blue-600 bg-blue-600 text-white"
                : "border-slate-300 bg-white text-slate-600"
            }`}
          >
            {c}
          </button>
        ))}
      </div>

      {loading && <p className="text-sm text-slate-400">Loading vendors...</p>}

      <div className="space-y-3">
        {vendors.map((v) => (
          <Link
            key={v.vendorId}
            to={`/vendors/${v.qrCode}`}
            className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm hover:bg-slate-50"
          >
            <div>
              <p className="font-medium text-slate-800">
                {v.businessName} {v.verified && <span className="text-blue-500">✓</span>}
              </p>
              <p className="text-xs text-slate-500">{v.locationName} · {v.category}</p>
            </div>
            <div className="text-right text-sm">
              <p className="font-semibold text-amber-500">★ {Number(v.ratingAvg).toFixed(1)}</p>
              <p className="text-xs text-slate-400">{v.ratingCount} reviews</p>
            </div>
          </Link>
        ))}
        {!loading && vendors.length === 0 && (
          <p className="text-sm text-slate-400">No vendors found in this category.</p>
        )}
      </div>
    </div>
  );
}
