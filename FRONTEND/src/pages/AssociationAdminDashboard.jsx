import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Building2, MapPin, Phone, ShieldCheck, User } from "lucide-react";
import client from "../api/client";
import { SkeletonCard } from "../components/ui/Skeleton";

export default function AssociationAdminDashboard() {
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    client.get("/users/me").then((res) => setProfile(res.data));
  }, []);

  if (!profile) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
      </div>
    );
  }

  const rows = [
    { icon: User, label: "Name", value: `${profile.name} ${profile.surname}` },
    { icon: Phone, label: "Cellphone", value: profile.phoneNumber },
    { icon: Building2, label: "Taxi association", value: profile.associationName || "—" },
    { icon: MapPin, label: "Taxi rank", value: profile.rankName || "—" },
  ];

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <p className="mb-1 text-xs font-medium uppercase tracking-wide text-terracotta-600">Association Administrator</p>
      <h1 className="mb-4 font-display text-xl text-sand-900">
        {profile.name} {profile.surname}
      </h1>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-2xl border border-sand-200 bg-white p-5 shadow-sm"
      >
        <div className="mb-4 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
          <ShieldCheck size={15} className="text-terracotta-600" /> Administrator profile
        </div>
        <div className="space-y-3">
          {rows.map((r) => (
            <div key={r.label} className="flex items-center justify-between border-b border-sand-100 pb-3 last:border-0 last:pb-0">
              <span className="flex items-center gap-2 text-sm text-sand-500">
                <r.icon size={14} /> {r.label}
              </span>
              <span className="text-sm font-medium text-sand-800">{r.value}</span>
            </div>
          ))}
        </div>
      </motion.div>

      <div className="mt-5 rounded-xl bg-sand-50 p-4 text-sm text-sand-600">
        Driver and vendor oversight for {profile.associationName || "your association"} — member management and
        rank-level reporting are next on the roadmap.
      </div>
    </div>
  );
}
