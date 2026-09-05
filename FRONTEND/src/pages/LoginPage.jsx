import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Lock, Phone } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import Button from "../components/ui/Button";
import { ease } from "../lib/motion";
import { dashboardPathFor } from "../lib/roles";
import logo from "../assets/Ukhona Logo.png";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [phoneNumber, setPhoneNumber] = useState("");
  const [pin, setPin] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const user = await login(phoneNumber, pin);
      navigate(dashboardPathFor(user.userType), { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative flex min-h-dvh items-center justify-center overflow-hidden bg-sand-50 px-4">
      {/* Lowveld skyline motif - stylised escarpment silhouette, not literal/kitsch */}
      <svg
        className="pointer-events-none absolute inset-x-0 bottom-0 h-40 w-full text-terracotta-100"
        viewBox="0 0 800 160"
        preserveAspectRatio="none"
        fill="currentColor"
        aria-hidden="true"
      >
        <path d="M0,160 L0,90 L90,55 L160,95 L230,40 L310,80 L390,20 L460,70 L540,45 L620,100 L700,60 L800,90 L800,160 Z" />
      </svg>
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: ease.enter }}
        className="relative w-full max-w-sm rounded-2xl border border-sand-200 bg-white p-8 shadow-warm-lg"
      >
        <div className="mb-7 text-center">
          <motion.img
            src={logo}
            alt="Ukhona Pay"
            initial={{ scale: 0.6, rotate: -8 }}
            animate={{ scale: 1, rotate: 0 }}
            transition={{ type: "spring", stiffness: 260, damping: 16, delay: 0.1 }}
            className="mx-auto mb-4 h-24 w-24 object-contain"
          />
          <h1 className="font-display text-2xl text-sand-900">Ukhona Pay</h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-sand-700">Phone number</label>
            <div className="relative">
              <Phone size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
              <input
                type="tel"
                placeholder="0798765432"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                className="w-full rounded-xl border border-sand-300 bg-sand-50/50 py-2.5 pl-10 pr-3 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                required
              />
            </div>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-sand-700">PIN</label>
            <div className="relative">
              <Lock size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
              <input
                type="password"
                inputMode="numeric"
                maxLength={4}
                placeholder="****"
                value={pin}
                onChange={(e) => setPin(e.target.value)}
                className="w-full rounded-xl border border-sand-300 bg-sand-50/50 py-2.5 pl-10 pr-3 tracking-[0.4em] text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                required
              />
            </div>
          </div>

          {error && (
            <motion.p initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} className="text-sm text-red-600">
              {error}
            </motion.p>
          )}

          <Button type="submit" loading={loading} className="w-full">
            Log in <ArrowRight size={16} />
          </Button>
        </form>

        <p className="mt-5 text-center text-sm text-sand-500">
          New here?{" "}
          <Link to="/signup" className="font-semibold text-terracotta-700">
            Create an account
          </Link>
        </p>
      </motion.div>
    </div>
  );
}
