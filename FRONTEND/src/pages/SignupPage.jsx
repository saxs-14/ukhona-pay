import { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import client from "../api/client";
import Button from "../components/ui/Button";
import { ease } from "../lib/motion";
import { dashboardPathFor } from "../lib/roles";

const ROLES = [
  { value: "TAXI_DRIVER", label: "Taxi Driver" },
  { value: "VENDOR", label: "Vendor" },
  { value: "TAXI_ASSOCIATION_ADMIN", label: "Taxi Association Administrator" },
];

export default function SignupPage() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [associations, setAssociations] = useState([]);
  const [ranks, setRanks] = useState([]);
  const [form, setForm] = useState({
    userType: "TAXI_DRIVER",
    name: "",
    surname: "",
    idNumber: "",
    phoneNumber: "",
    pin: "",
    email: "",
    vehicleRegistration: "",
    associationId: "",
    rankId: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([client.get("/taxi-associations"), client.get("/taxi-ranks")]).then(([a, r]) => {
      setAssociations(a.data);
      setRanks(r.data);
    });
  }, []);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const payload = {
        userType: form.userType,
        name: form.name,
        surname: form.surname,
        idNumber: form.idNumber,
        phoneNumber: form.phoneNumber,
        pin: form.pin,
        email: form.email || null,
        vehicleRegistration: form.userType === "TAXI_DRIVER" ? form.vehicleRegistration : null,
        associationId: form.associationId ? Number(form.associationId) : null,
        rankId: form.rankId ? Number(form.rankId) : null,
      };
      const user = await signup(payload);
      navigate(dashboardPathFor(user.userType));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  const inputClass =
    "w-full rounded-xl border border-sand-300 bg-sand-50/50 px-3.5 py-2.5 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100";

  return (
    <div className="flex min-h-dvh items-center justify-center bg-sand-50 px-4 py-8">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: ease.enter }}
        className="w-full max-w-sm rounded-2xl border border-sand-200 bg-white p-8 shadow-warm-lg"
      >
        <h1 className="mb-1 font-display text-2xl text-sand-900">Create your account</h1>
        <p className="mb-6 text-sm text-sand-500">Financial identity for Mbombela's taxi-rank traders</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-sand-700">I am a</label>
            <select
              value={form.userType}
              onChange={(e) => update("userType", e.target.value)}
              className={inputClass}
            >
              {ROLES.map((r) => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Name" value={form.name} onChange={(e) => update("name", e.target.value)} className={inputClass} required />
            <input placeholder="Surname" value={form.surname} onChange={(e) => update("surname", e.target.value)} className={inputClass} required />
          </div>

          <input
            placeholder="ID number"
            inputMode="numeric"
            maxLength={13}
            value={form.idNumber}
            onChange={(e) => update("idNumber", e.target.value.replace(/\D/g, ""))}
            className={inputClass}
            required
          />

          <input
            placeholder="Cellphone number (0XXXXXXXXX)"
            value={form.phoneNumber}
            onChange={(e) => update("phoneNumber", e.target.value)}
            className={inputClass}
            required
          />

          <input
            type="password"
            inputMode="numeric"
            maxLength={4}
            placeholder="4-digit PIN"
            value={form.pin}
            onChange={(e) => update("pin", e.target.value)}
            className={inputClass}
            required
          />

          <input type="email" placeholder="Email (optional)" value={form.email} onChange={(e) => update("email", e.target.value)} className={inputClass} />

          {form.userType === "TAXI_DRIVER" && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="space-y-4 overflow-hidden">
              <input
                placeholder="Vehicle registration"
                value={form.vehicleRegistration}
                onChange={(e) => update("vehicleRegistration", e.target.value.toUpperCase())}
                className={inputClass}
                required
              />
              <div>
                <label className="mb-1.5 block text-sm font-medium text-sand-700">Taxi association</label>
                <select value={form.associationId} onChange={(e) => update("associationId", e.target.value)} className={inputClass} required>
                  <option value="" disabled>Select the association your taxi belongs to</option>
                  {associations.map((a) => (
                    <option key={a.id} value={a.id}>{a.name}</option>
                  ))}
                </select>
              </div>
            </motion.div>
          )}

          {form.userType === "VENDOR" && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="overflow-hidden">
              <label className="mb-1.5 block text-sm font-medium text-sand-700">Taxi rank</label>
              <select value={form.rankId} onChange={(e) => update("rankId", e.target.value)} className={inputClass} required>
                <option value="" disabled>Select the rank you trade at</option>
                {ranks.map((r) => (
                  <option key={r.id} value={r.id}>{r.name}</option>
                ))}
              </select>
            </motion.div>
          )}

          {form.userType === "TAXI_ASSOCIATION_ADMIN" && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="space-y-4 overflow-hidden">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-sand-700">Taxi association</label>
                <select value={form.associationId} onChange={(e) => update("associationId", e.target.value)} className={inputClass} required>
                  <option value="" disabled>Search the association you work for</option>
                  {associations.map((a) => (
                    <option key={a.id} value={a.id}>{a.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-sand-700">Taxi rank</label>
                <select value={form.rankId} onChange={(e) => update("rankId", e.target.value)} className={inputClass} required>
                  <option value="" disabled>Select the rank you oversee</option>
                  {ranks.map((r) => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>
            </motion.div>
          )}

          {error && <p className="text-sm text-red-600">{error}</p>}

          <Button type="submit" loading={loading} className="w-full">
            Sign up <ArrowRight size={16} />
          </Button>
        </form>

        <p className="mt-5 text-center text-sm text-sand-500">
          Already have an account?{" "}
          <Link to="/login" className="font-semibold text-terracotta-700">
            Log in
          </Link>
        </p>
      </motion.div>
    </div>
  );
}
