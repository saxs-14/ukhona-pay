import { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import client from "../api/client";
import Button from "../components/ui/Button";
import { ease } from "../lib/motion";
import { dashboardPathFor } from "../lib/roles";
import { isValidName, isValidSaId, isValidVehicleReg, isWeakPin, normalizeVehicleReg } from "../lib/validators";

const ROLES = [
  { value: "TAXI_DRIVER", label: "Taxi Driver" },
  { value: "VENDOR", label: "Vendor" },
  { value: "TAXI_ASSOCIATION_ADMIN", label: "Taxi Association Administrator" },
];

const inputClass =
  "w-full rounded-xl border border-sand-300 bg-sand-50/50 px-3.5 py-2.5 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100";
const errorInputClass = "border-red-400 focus:border-red-500 focus:ring-red-100";

function Field({ label, required, hint, error, children }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-sand-700">
        {label} {required ? <span className="text-terracotta-600">*</span> : <span className="font-normal text-sand-400">(optional)</span>}
      </label>
      {children}
      {error ? (
        <p className="mt-1 text-xs text-red-600">{error}</p>
      ) : hint ? (
        <p className="mt-1 text-xs text-sand-400">{hint}</p>
      ) : null}
    </div>
  );
}

// Resolves a typed name to an existing entity's id (case-insensitive match
// against what's already on the platform) or creates a new one. There's no
// pre-loaded list of associations/ranks - the first real user to need one
// creates it, everyone after reuses it.
async function resolveOrCreate(path, name, extra = {}) {
  const { data } = await client.post(path, { name: name.trim(), ...extra });
  return data.id;
}

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
    associationName: "",
    rankName: "",
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    refreshLists();
  }, []);

  function refreshLists() {
    Promise.all([client.get("/taxi-associations"), client.get("/taxi-ranks")]).then(([a, r]) => {
      setAssociations(a.data);
      setRanks(r.data);
    });
  }

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    if (fieldErrors[field]) setFieldErrors((fe) => ({ ...fe, [field]: null }));
  }

  function validate() {
    const errors = {};
    if (!isValidName(form.name)) errors.name = "Letters only, 2-60 characters";
    if (!isValidName(form.surname)) errors.surname = "Letters only, 2-60 characters";
    if (!isValidSaId(form.idNumber)) errors.idNumber = "Not a valid South African ID number";
    if (!/^0[0-9]{9}$/.test(form.phoneNumber)) errors.phoneNumber = "10-digit SA number starting with 0";
    if (!/^[0-9]{4}$/.test(form.pin)) errors.pin = "Must be exactly 4 digits";
    else if (isWeakPin(form.pin)) errors.pin = "Too easy to guess - avoid repeated or sequential digits";
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = "Not a valid email address";

    if (form.userType === "TAXI_DRIVER") {
      if (!isValidVehicleReg(form.vehicleRegistration)) {
        errors.vehicleRegistration = "Not a valid SA number plate, e.g. DX45FGMP or CA123456";
      }
      if (form.associationName.trim().length < 3) errors.associationName = "Enter your taxi association's name";
    }
    if (form.userType === "VENDOR" && form.rankName.trim().length < 3) {
      errors.rankName = "Enter the rank you trade at";
    }
    if (form.userType === "TAXI_ASSOCIATION_ADMIN") {
      if (form.associationName.trim().length < 3) errors.associationName = "Enter the association you work for";
      if (form.rankName.trim().length < 3) errors.rankName = "Enter the rank you oversee";
    }
    return errors;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setLoading(true);
    try {
      let associationId = null;
      let rankId = null;
      if (form.userType === "TAXI_DRIVER" || form.userType === "TAXI_ASSOCIATION_ADMIN") {
        associationId = await resolveOrCreate("/taxi-associations", form.associationName);
      }
      if (form.userType === "VENDOR" || form.userType === "TAXI_ASSOCIATION_ADMIN") {
        rankId = await resolveOrCreate("/taxi-ranks", form.rankName);
      }

      const payload = {
        userType: form.userType,
        name: form.name,
        surname: form.surname,
        idNumber: form.idNumber,
        phoneNumber: form.phoneNumber,
        pin: form.pin,
        email: form.email || null,
        vehicleRegistration: form.userType === "TAXI_DRIVER" ? normalizeVehicleReg(form.vehicleRegistration) : null,
        associationId,
        rankId,
      };
      const user = await signup(payload);
      navigate(dashboardPathFor(user.userType));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function inputCls(field) {
    return `${inputClass} ${fieldErrors[field] ? errorInputClass : ""}`;
  }

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

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <Field label="I am a" required>
            <select value={form.userType} onChange={(e) => update("userType", e.target.value)} className={inputClass}>
              {ROLES.map((r) => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </Field>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Name" required error={fieldErrors.name}>
              <input value={form.name} onChange={(e) => update("name", e.target.value)} className={inputCls("name")} />
            </Field>
            <Field label="Surname" required error={fieldErrors.surname}>
              <input value={form.surname} onChange={(e) => update("surname", e.target.value)} className={inputCls("surname")} />
            </Field>
          </div>

          <Field label="ID number" required hint="13-digit South African ID number" error={fieldErrors.idNumber}>
            <input
              inputMode="numeric"
              maxLength={13}
              value={form.idNumber}
              onChange={(e) => update("idNumber", e.target.value.replace(/\D/g, ""))}
              className={inputCls("idNumber")}
            />
          </Field>

          <Field label="Cellphone number" required hint="e.g. 0798765432" error={fieldErrors.phoneNumber}>
            <input value={form.phoneNumber} onChange={(e) => update("phoneNumber", e.target.value)} className={inputCls("phoneNumber")} />
          </Field>

          <Field label="PIN" required hint="4 digits, avoid obvious patterns like 1234" error={fieldErrors.pin}>
            <input
              type="password"
              inputMode="numeric"
              maxLength={4}
              value={form.pin}
              onChange={(e) => update("pin", e.target.value.replace(/\D/g, ""))}
              className={inputCls("pin")}
            />
          </Field>

          <Field label="Email" error={fieldErrors.email}>
            <input type="email" value={form.email} onChange={(e) => update("email", e.target.value)} className={inputCls("email")} />
          </Field>

          {form.userType === "TAXI_DRIVER" && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="space-y-4 overflow-hidden">
              <Field label="Vehicle registration" required hint="South African number plate, e.g. DX45FGMP or CA123456" error={fieldErrors.vehicleRegistration}>
                <input
                  value={form.vehicleRegistration}
                  onChange={(e) => update("vehicleRegistration", e.target.value.toUpperCase())}
                  className={inputCls("vehicleRegistration")}
                />
              </Field>
              <Field label="Taxi association" required hint="Start typing - pick your association if it's listed, or enter its name to register it" error={fieldErrors.associationName}>
                <input
                  list="association-options"
                  value={form.associationName}
                  onChange={(e) => update("associationName", e.target.value)}
                  className={inputCls("associationName")}
                />
                <datalist id="association-options">
                  {associations.map((a) => (
                    <option key={a.id} value={a.name} />
                  ))}
                </datalist>
              </Field>
            </motion.div>
          )}

          {form.userType === "VENDOR" && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="overflow-hidden">
              <Field label="Taxi rank" required hint="Start typing - pick your rank if it's listed, or enter its name to register it" error={fieldErrors.rankName}>
                <input
                  list="rank-options"
                  value={form.rankName}
                  onChange={(e) => update("rankName", e.target.value)}
                  className={inputCls("rankName")}
                />
                <datalist id="rank-options">
                  {ranks.map((r) => (
                    <option key={r.id} value={r.name} />
                  ))}
                </datalist>
              </Field>
            </motion.div>
          )}

          {form.userType === "TAXI_ASSOCIATION_ADMIN" && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="space-y-4 overflow-hidden">
              <Field label="Taxi association" required hint="Start typing - pick your association if it's listed, or enter its name to register it" error={fieldErrors.associationName}>
                <input
                  list="association-options"
                  value={form.associationName}
                  onChange={(e) => update("associationName", e.target.value)}
                  className={inputCls("associationName")}
                />
                <datalist id="association-options">
                  {associations.map((a) => (
                    <option key={a.id} value={a.name} />
                  ))}
                </datalist>
              </Field>
              <Field label="Taxi rank" required hint="Start typing - pick the rank if it's listed, or enter its name to register it" error={fieldErrors.rankName}>
                <input
                  list="rank-options"
                  value={form.rankName}
                  onChange={(e) => update("rankName", e.target.value)}
                  className={inputCls("rankName")}
                />
                <datalist id="rank-options">
                  {ranks.map((r) => (
                    <option key={r.id} value={r.name} />
                  ))}
                </datalist>
              </Field>
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
