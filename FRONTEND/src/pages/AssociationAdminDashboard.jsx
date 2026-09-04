import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Building2, Clock, MapPin, Phone, ShieldCheck, User, Wallet } from "lucide-react";
import client from "../api/client";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem } from "../lib/motion";

export default function AssociationAdminDashboard() {
  const [profile, setProfile] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [transfers, setTransfers] = useState([]);

  useEffect(() => {
    client.get("/users/me").then((res) => setProfile(res.data));
    client.get("/wallet/association/me").then((res) => setWallet(res.data)).catch(() => setWallet(null));
    client.get("/transactions/association/me").then((res) => setTransfers(res.data)).catch(() => setTransfers([]));
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

      {wallet && (
        <div className="mb-3 rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-4 text-white shadow-warm">
          <p className="flex items-center gap-1.5 text-xs text-terracotta-100">
            <Wallet size={12} /> Association wallet balance
          </p>
          <p className="text-2xl font-semibold">
            <AnimatedNumber value={Number(wallet.balance)} prefix="R" />
          </p>
          <p className="mt-1 text-xs text-terracotta-100">From drivers paying their taxi owner/association directly</p>
        </div>
      )}

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

      <div className="mt-6">
        <h2 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
          <Clock size={14} /> Recent transfers from drivers
        </h2>
        <motion.div variants={listContainer} initial="initial" animate="animate" className="space-y-2">
          {transfers.map((t) => (
            <motion.div key={t.reference} variants={listItem} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3">
              <div>
                <p className="text-sm font-medium text-sand-800">{t.senderName}</p>
                <p className="text-xs text-sand-400">{new Date(t.createdAt).toLocaleString("en-ZA")}</p>
              </div>
              <p className="text-sm font-semibold text-bushveld-600">+R{Number(t.amount).toFixed(2)}</p>
            </motion.div>
          ))}
          {transfers.length === 0 && <p className="text-sm text-sand-400">No transfers received yet.</p>}
        </motion.div>
      </div>

      <div className="mt-6 rounded-xl bg-sand-50 p-4 text-sm text-sand-600">
        Driver and vendor oversight for {profile.associationName || "your association"} — member management and
        rank-level reporting are next on the roadmap.
      </div>
    </div>
  );
}
