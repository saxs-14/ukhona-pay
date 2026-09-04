import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Smartphone, Store } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import Button from "../components/ui/Button";
import { ease, spring } from "../lib/motion";

const CATEGORIES = [
  { value: "TAXI", label: "TAXI (Driver)" },
  { value: "FOOD", label: "FOOD" },
  { value: "SERVICES", label: "SERVICES" },
  { value: "RETAIL", label: "RETAIL" },
  { value: "OTHER", label: "OTHER" },
];

export default function SignupPage() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: "",
    phoneNumber: "",
    pin: "",
    email: "",
    userType: "EMPLOYEE",
    businessName: "",
    category: "TAXI",
    locationName: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const user = await signup(form);
      navigate(user.userType === "VENDOR" ? "/vendor" : "/dashboard");
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
          <div className="flex gap-2">
            {[
              { key: "EMPLOYEE", label: "Commuter (demo)", Icon: Smartphone },
              { key: "VENDOR", label: "Driver / Vendor", Icon: Store },
            ].map(({ key, label, Icon }) => {
              const active = form.userType === key;
              return (
                <motion.button
                  type="button"
                  key={key}
                  whileTap={{ scale: 0.97 }}
                  transition={spring}
                  onClick={() => update("userType", key)}
                  className={`flex flex-1 items-center justify-center gap-1.5 rounded-xl border py-2.5 text-sm font-medium transition-colors ${
                    active
                      ? "border-terracotta-600 bg-terracotta-50 text-terracotta-700"
                      : "border-sand-300 text-sand-600"
                  }`}
                >
                  <Icon size={15} />
                  {label}
                </motion.button>
              );
            })}
          </div>
          {form.userType === "EMPLOYEE" && (
            <p className="rounded-lg bg-sand-50 px-3 py-2 text-xs text-sand-500">
              In production, commuters pay with their own banking app — no UKHONA PAY account
              needed. This demo login simulates that payment for testing.
            </p>
          )}

          <input placeholder="Full name" value={form.name} onChange={(e) => update("name", e.target.value)} className={inputClass} required />
          <input
            placeholder="Phone number (0XXXXXXXXX)"
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

          {form.userType === "VENDOR" && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="space-y-4 overflow-hidden">
              <input
                placeholder="Business name"
                value={form.businessName}
                onChange={(e) => update("businessName", e.target.value)}
                className={inputClass}
                required
              />
              <select value={form.category} onChange={(e) => update("category", e.target.value)} className={inputClass}>
                {CATEGORIES.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
              <input
                placeholder="Location (e.g. KaNyamazane, Mbombela)"
                value={form.locationName}
                onChange={(e) => update("locationName", e.target.value)}
                className={inputClass}
                required
              />
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
