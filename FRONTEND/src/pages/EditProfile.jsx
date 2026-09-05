import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowLeft, Building2, Car, Check, Mail, Save, User, UserCog } from "lucide-react";
import client from "../api/client";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import { SkeletonCard } from "../components/ui/Skeleton";
import { ease } from "../lib/motion";

const inputClass =
  "w-full rounded-xl border border-sand-300 bg-sand-50/50 px-3.5 py-2.5 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100";

function Field({ label, icon: Icon, children }) {
  return (
    <div>
      <label className="mb-1.5 flex items-center gap-1.5 text-sm font-medium text-sand-700">
        {Icon && <Icon size={13} className="text-sand-400" />} {label}
      </label>
      {children}
    </div>
  );
}

export default function EditProfile() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const [hasVendorProfile, setHasVendorProfile] = useState(false);
  const [isDriver, setIsDriver] = useState(false);

  const [name, setName] = useState("");
  const [surname, setSurname] = useState("");
  const [email, setEmail] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [vehicleRegistration, setVehicleRegistration] = useState("");

  useEffect(() => {
    Promise.all([client.get("/users/me"), client.get("/vendors/me").catch(() => null)])
      .then(([u, v]) => {
        setName(u.data.name);
        setSurname(u.data.surname);
        setEmail(u.data.email || "");
        if (v) {
          setHasVendorProfile(true);
          setBusinessName(v.data.businessName);
          if (v.data.vehicleRegistration) {
            setIsDriver(true);
            setVehicleRegistration(v.data.vehicleRegistration);
          }
        }
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSave(e) {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      await client.put("/users/me", { name, surname, email: email || null });
      if (hasVendorProfile) {
        await client.put("/vendors/me", {
          businessName,
          vehicleRegistration: isDriver ? vehicleRegistration : null,
        });
      }
      setSaved(true);
      setTimeout(() => setSaved(false), 2500);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <Link to="/" className="mb-3 inline-flex items-center gap-1 text-sm font-medium text-sand-500 hover:text-terracotta-700">
        <ArrowLeft size={15} /> Back
      </Link>
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <UserCog size={20} className="text-terracotta-600" /> Edit Profile
      </h1>
      <p className="mb-4 text-sm text-sand-500">
        Fix a typo or update your own details - phone number, ID number, and account type stay
        with your association for verification.
      </p>

      <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, ease: ease.enter }}>
        <Card>
          <form onSubmit={handleSave} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <Field label="Name" icon={User}>
                <input required value={name} onChange={(e) => setName(e.target.value)} className={inputClass} />
              </Field>
              <Field label="Surname" icon={User}>
                <input required value={surname} onChange={(e) => setSurname(e.target.value)} className={inputClass} />
              </Field>
            </div>

            <Field label="Email (optional)" icon={Mail}>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className={inputClass}
              />
            </Field>

            {hasVendorProfile && (
              <>
                <div className="border-t border-sand-200 pt-4">
                  <Field label={isDriver ? "Taxi/Business Name" : "Business Name"} icon={Building2}>
                    <input
                      required
                      value={businessName}
                      onChange={(e) => setBusinessName(e.target.value)}
                      className={inputClass}
                    />
                  </Field>
                </div>

                {isDriver && (
                  <Field label="Vehicle Registration" icon={Car}>
                    <input
                      required
                      value={vehicleRegistration}
                      onChange={(e) => setVehicleRegistration(e.target.value.toUpperCase())}
                      placeholder="DX 45 FG MP"
                      className={`${inputClass} font-mono tracking-wider`}
                    />
                    <p className="mt-1 text-xs text-sand-400">
                      Your taxi association can see this changed on your profile - only update it if the
                      registration was wrong or you've changed vehicles.
                    </p>
                  </Field>
                )}
              </>
            )}

            {error && <p className="text-sm text-red-600">{error}</p>}

            <Button type="submit" loading={saving} className="w-full">
              {saved ? (
                <>
                  <Check size={16} /> Saved
                </>
              ) : (
                <>
                  <Save size={16} /> Save changes
                </>
              )}
            </Button>
          </form>
        </Card>
      </motion.div>
    </div>
  );
}
