import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { ArrowLeft, Car, Check, Hourglass, Phone, Search, ShieldAlert, Users, X } from "lucide-react";
import client from "../api/client";
import { SkeletonCard } from "../components/ui/Skeleton";
import { spring } from "../lib/motion";

const STATUS_BADGE = {
  APPROVED: "bg-bushveld-100 text-bushveld-700",
  PENDING: "bg-gold-100 text-gold-700",
  REJECTED: "bg-red-100 text-red-600",
};

export default function AssociationAdminDrivers() {
  const [associationName, setAssociationName] = useState("");
  const [pendingDrivers, setPendingDrivers] = useState([]);
  const [roster, setRoster] = useState([]);
  const [loading, setLoading] = useState(true);
  const [decidingId, setDecidingId] = useState(null);
  const [decisionError, setDecisionError] = useState("");
  const [search, setSearch] = useState("");

  useEffect(() => {
    client.get("/users/me").then((res) => setAssociationName(res.data.associationName));
    loadDrivers();
  }, []);

  function loadDrivers() {
    Promise.all([
      client.get("/vendors/pending").then((res) => res.data).catch(() => []),
      client.get("/vendors/association/roster").then((res) => res.data).catch(() => []),
    ])
      .then(([pending, all]) => {
        setPendingDrivers(pending);
        setRoster(all);
      })
      .finally(() => setLoading(false));
  }

  async function decide(vendorId, decision) {
    setDecisionError("");
    setDecidingId(vendorId);
    try {
      await client.post(`/vendors/${vendorId}/${decision}`);
      setPendingDrivers((prev) => prev.filter((d) => d.vendorId !== vendorId));
      setRoster((prev) =>
        prev.map((d) => (d.vendorId === vendorId ? { ...d, status: decision === "approve" ? "APPROVED" : "REJECTED" } : d))
      );
    } catch (err) {
      setDecisionError(err.message);
    } finally {
      setDecidingId(null);
    }
  }

  const filteredRoster = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return roster;
    return roster.filter(
      (d) =>
        `${d.name} ${d.surname}`.toLowerCase().includes(q) ||
        d.phoneNumber.includes(q) ||
        (d.vehicleRegistration || "").toLowerCase().includes(q)
    );
  }, [roster, search]);

  if (loading) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <Link to="/association-admin" className="mb-3 inline-flex items-center gap-1 text-sm font-medium text-sand-500 hover:text-terracotta-700">
        <ArrowLeft size={15} /> Back
      </Link>
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <Users size={20} className="text-terracotta-600" /> Drivers
      </h1>
      <p className="mb-4 text-sm text-sand-500">{associationName || "Your association"}</p>

      {decisionError && (
        <div className="mb-3 flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <ShieldAlert size={15} /> {decisionError}
        </div>
      )}

      {pendingDrivers.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-4 rounded-2xl border border-gold-200 bg-gold-50 p-4 shadow-sm"
        >
          <div className="mb-3 flex items-center gap-1.5 text-sm font-semibold text-gold-800">
            <Hourglass size={15} /> Awaiting your review ({pendingDrivers.length})
          </div>
          <div className="space-y-2">
            <AnimatePresence initial={false}>
              {pendingDrivers.map((d) => (
                <motion.div
                  key={d.vendorId}
                  layout
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, height: 0, marginBottom: 0, paddingTop: 0, paddingBottom: 0 }}
                  transition={spring}
                  className="rounded-xl border border-gold-200/70 bg-white p-3"
                >
                  <p className="text-sm font-semibold text-sand-800">{d.name} {d.surname}</p>
                  <p className="mt-0.5 flex items-center gap-1 text-xs text-sand-500">
                    <Phone size={11} /> {d.phoneNumber}
                  </p>
                  <p className="mt-0.5 flex items-center gap-1 text-xs text-sand-500">
                    <Car size={11} /> {d.vehicleRegistration}
                  </p>
                  <p className="mt-1 text-[11px] text-sand-400">
                    Verify the vehicle is genuinely registered with your association before approving.
                  </p>
                  <div className="mt-3 flex gap-2">
                    <button
                      disabled={decidingId === d.vendorId}
                      onClick={() => decide(d.vendorId, "approve")}
                      className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-bushveld-600 py-2 text-xs font-semibold text-white transition-colors hover:bg-bushveld-700 disabled:opacity-50"
                    >
                      <Check size={13} /> Approve
                    </button>
                    <button
                      disabled={decidingId === d.vendorId}
                      onClick={() => decide(d.vendorId, "reject")}
                      className="flex flex-1 items-center justify-center gap-1.5 rounded-lg border border-red-200 bg-white py-2 text-xs font-semibold text-red-600 transition-colors hover:bg-red-50 disabled:opacity-50"
                    >
                      <X size={13} /> Reject
                    </button>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        </motion.div>
      )}

      <div className="relative mb-3">
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by name, phone, or plate"
          className="w-full rounded-xl border border-sand-300 bg-white py-2.5 pl-10 pr-3 text-sm text-sand-900 transition-colors focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
        />
      </div>

      <div className="rounded-2xl border border-sand-200 bg-white p-4 shadow-sm">
        <p className="mb-3 text-xs font-medium uppercase tracking-wide text-sand-400">
          All drivers ({filteredRoster.length})
        </p>
        {filteredRoster.length === 0 ? (
          <p className="py-6 text-center text-sm text-sand-400">No drivers match.</p>
        ) : (
          <div className="space-y-2">
            {filteredRoster.map((d) => (
              <div key={d.vendorId} className="flex items-center justify-between gap-3 rounded-xl border border-sand-100 bg-sand-50/50 px-3 py-2.5">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-sand-800">{d.name} {d.surname}</p>
                  <p className="truncate text-xs text-sand-400">{d.phoneNumber} · {d.vehicleRegistration}</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${STATUS_BADGE[d.status] || "bg-sand-100 text-sand-600"}`}>
                    {d.status}
                  </span>
                  {d.status !== "APPROVED" && (
                    <button
                      disabled={decidingId === d.vendorId}
                      onClick={() => decide(d.vendorId, "approve")}
                      className="text-xs font-semibold text-bushveld-600 hover:underline disabled:opacity-50"
                    >
                      Approve
                    </button>
                  )}
                  {d.status !== "REJECTED" && (
                    <button
                      disabled={decidingId === d.vendorId}
                      onClick={() => decide(d.vendorId, "reject")}
                      className="text-xs font-semibold text-red-600 hover:underline disabled:opacity-50"
                    >
                      {d.status === "APPROVED" ? "Revoke" : "Reject"}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
