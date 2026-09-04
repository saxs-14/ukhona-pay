import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { BadgeCheck, MapPin, Star } from "lucide-react";
import client from "../api/client";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem, spring } from "../lib/motion";

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
      <h1 className="mb-4 font-display text-xl text-sand-900">Find a vendor</h1>

      <div className="mb-4 flex gap-2 overflow-x-auto pb-1">
        {CATEGORIES.map((c) => {
          const active = category === c;
          return (
            <motion.button
              key={c}
              whileTap={{ scale: 0.94 }}
              transition={spring}
              onClick={() => setCategory(c)}
              className={`whitespace-nowrap rounded-full border px-3.5 py-1.5 text-sm transition-colors ${
                active ? "border-terracotta-600 bg-terracotta-600 text-white" : "border-sand-300 bg-white text-sand-600"
              }`}
            >
              {c}
            </motion.button>
          );
        })}
      </div>

      {loading && (
        <div className="space-y-3">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      )}

      <motion.div variants={listContainer} initial="initial" animate="animate" className="space-y-3">
        {vendors.map((v) => (
          <motion.div key={v.vendorId} variants={listItem} whileTap={{ scale: 0.98 }}>
            <Link
              to={`/vendors/${v.qrCode}`}
              className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3 shadow-sm hover:border-terracotta-200"
            >
              <div>
                <p className="flex items-center gap-1 font-medium text-sand-800">
                  {v.businessName}
                  {v.verified && <BadgeCheck size={15} className="text-terracotta-600" />}
                </p>
                <p className="flex items-center gap-1 text-xs text-sand-500">
                  <MapPin size={11} /> {v.locationName} · {v.category}
                </p>
              </div>
              <div className="text-right text-sm">
                <p className="flex items-center justify-end gap-1 font-semibold text-gold-600">
                  <Star size={13} fill="#E2971E" strokeWidth={0} /> {Number(v.ratingAvg).toFixed(1)}
                </p>
                <p className="text-xs text-sand-400">{v.ratingCount} reviews</p>
              </div>
            </Link>
          </motion.div>
        ))}
        {!loading && vendors.length === 0 && <p className="text-sm text-sand-400">No vendors found in this category.</p>}
      </motion.div>
    </div>
  );
}
