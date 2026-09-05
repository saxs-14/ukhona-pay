import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Banknote, Building2, Car, Check, Clock, Hourglass, MapPin, Phone, Save, ShieldCheck, User, Wallet, X } from "lucide-react";
import client from "../api/client";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem, spring } from "../lib/motion";

export default function AssociationAdminDashboard() {
  const [profile, setProfile] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [transfers, setTransfers] = useState([]);
  const [pendingDrivers, setPendingDrivers] = useState([]);
  const [decidingId, setDecidingId] = useState(null);
  const [decisionError, setDecisionError] = useState("");
  const [duesAmount, setDuesAmount] = useState("");
  const [duesSaving, setDuesSaving] = useState(false);
  const [duesSaved, setDuesSaved] = useState(false);
  const [duesError, setDuesError] = useState("");

  useEffect(() => {
    client.get("/users/me").then((res) => setProfile(res.data));
    client.get("/wallet/association/me").then((res) => setWallet(res.data)).catch(() => setWallet(null));
    client.get("/transactions/association/me").then((res) => setTransfers(res.data)).catch(() => setTransfers([]));
    client.get("/vendors/pending").then((res) => setPendingDrivers(res.data)).catch(() => setPendingDrivers([]));
    client.get("/users/me").then((res) => {
      if (!res.data.associationId) return;
      client.get("/taxi-associations").then((assoc) => {
        const mine = assoc.data.find((a) => a.id === res.data.associationId);
        if (mine) setDuesAmount(String(mine.duesAmount));
      });
    });
  }, []);

  async function saveDues(e) {
    e.preventDefault();
    setDuesError("");
    setDuesSaving(true);
    try {
      await client.put("/vendors/association/dues", { duesAmount: Number(duesAmount) });
      setDuesSaved(true);
      setTimeout(() => setDuesSaved(false), 2000);
    } catch (err) {
      setDuesError(err.message);
    } finally {
      setDuesSaving(false);
    }
  }

  async function decide(vendorId, decision) {
    setDecisionError("");
    setDecidingId(vendorId);
    try {
      await client.post(`/vendors/${vendorId}/${decision}`);
      setPendingDrivers((prev) => prev.filter((d) => d.vendorId !== vendorId));
    } catch (err) {
      setDecisionError(err.message);
    } finally {
      setDecidingId(null);
    }
  }

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
        className="mt-3 rounded-2xl border border-sand-200 bg-white p-5 shadow-sm"
      >
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-sm font-semibold text-sand-700">
            <Hourglass size={15} className="text-gold-600" /> Driver approvals
          </div>
          {pendingDrivers.length > 0 && (
            <span className="rounded-full bg-gold-100 px-2 py-0.5 text-xs font-semibold text-gold-700">
              {pendingDrivers.length} pending
            </span>
          )}
        </div>

        {decisionError && <p className="mb-3 text-sm text-red-600">{decisionError}</p>}

        {pendingDrivers.length === 0 ? (
          <p className="text-sm text-sand-400">No drivers awaiting review.</p>
        ) : (
          <div className="space-y-3">
            <AnimatePresence initial={false}>
              {pendingDrivers.map((d) => (
                <motion.div
                  key={d.vendorId}
                  layout
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, height: 0, marginBottom: 0, paddingTop: 0, paddingBottom: 0 }}
                  transition={spring}
                  className="rounded-xl border border-sand-200 bg-sand-50/60 p-3"
                >
                  <p className="text-sm font-semibold text-sand-800">
                    {d.name} {d.surname}
                  </p>
                  <p className="mt-0.5 flex items-center gap-1 text-xs text-sand-500">
                    <Phone size={11} /> {d.phoneNumber}
                  </p>
                  <p className="mt-0.5 flex items-center gap-1 text-xs text-sand-500">
                    <Car size={11} /> {d.vehicleRegistration}
                  </p>
                  <p className="mt-0.5 text-[11px] text-sand-400">
                    Registered {new Date(d.registeredAt).toLocaleDateString("en-ZA")} — verify the vehicle is
                    registered with your association before approving.
                  </p>
                  <div className="mt-3 flex gap-2">
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      disabled={decidingId === d.vendorId}
                      onClick={() => decide(d.vendorId, "approve")}
                      className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-bushveld-600 py-2 text-xs font-semibold text-white transition-colors hover:bg-bushveld-700 disabled:opacity-50"
                    >
                      <Check size={13} /> Approve
                    </motion.button>
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      disabled={decidingId === d.vendorId}
                      onClick={() => decide(d.vendorId, "reject")}
                      className="flex flex-1 items-center justify-center gap-1.5 rounded-lg border border-red-200 bg-white py-2 text-xs font-semibold text-red-600 transition-colors hover:bg-red-50 disabled:opacity-50"
                    >
                      <X size={13} /> Reject
                    </motion.button>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.03 }}
        className="mt-3 rounded-2xl border border-sand-200 bg-white p-5 shadow-sm"
      >
        <div className="mb-1 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
          <Banknote size={15} className="text-terracotta-600" /> Membership dues
        </div>
        <p className="mb-3 text-xs text-sand-500">
          A once-off amount your association charges drivers - set it now, then revisit only occasionally
          (e.g. yearly) as costs change. Drivers see this pre-filled when paying your association.
        </p>
        <form onSubmit={saveDues} className="flex items-center gap-2">
          <div className="relative flex-1">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400">R</span>
            <input
              required
              type="number"
              min="0"
              step="0.01"
              value={duesAmount}
              onChange={(e) => setDuesAmount(e.target.value)}
              className="w-full rounded-xl border border-sand-300 bg-sand-50/50 py-2.5 pl-7 pr-3 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100"
            />
          </div>
          <motion.button
            whileTap={{ scale: 0.95 }}
            type="submit"
            disabled={duesSaving}
            className="flex items-center gap-1.5 rounded-xl bg-terracotta-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-terracotta-700 disabled:opacity-50"
          >
            {duesSaved ? <Check size={15} /> : <Save size={15} />}
            {duesSaved ? "Saved" : "Save"}
          </motion.button>
        </form>
        {duesError && <p className="mt-2 text-sm text-red-600">{duesError}</p>}
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.05 }}
        className="mt-3 rounded-2xl border border-sand-200 bg-white p-5 shadow-sm"
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
