import { useEffect, useState } from "react";
import { Building2, MapPin, Pencil, ShieldCheck, Trash2, Users as UsersIcon, X } from "lucide-react";
import client from "../api/client";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import { SkeletonCard } from "../components/ui/Skeleton";

const inputClass =
  "w-full rounded-xl border border-sand-300 bg-sand-50/50 px-3.5 py-2.5 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100";

function Field({ label, children }) {
  return (
    <div>
      <label className="mb-1 block text-xs font-medium text-sand-700">{label}</label>
      {children}
    </div>
  );
}

const TABS = [
  { key: "users", label: "Users", icon: UsersIcon },
  { key: "vendors", label: "Vendors & drivers", icon: ShieldCheck },
  { key: "associations", label: "Taxi associations", icon: Building2 },
  { key: "ranks", label: "Taxi ranks", icon: MapPin },
];

export default function AdminDashboard() {
  const [tab, setTab] = useState("users");
  const [users, setUsers] = useState(null);
  const [vendors, setVendors] = useState(null);
  const [associations, setAssociations] = useState(null);
  const [ranks, setRanks] = useState(null);
  const [error, setError] = useState("");

  function reloadAll() {
    client.get("/admin/users").then((r) => setUsers(r.data));
    client.get("/admin/vendors").then((r) => setVendors(r.data));
    client.get("/taxi-associations").then((r) => setAssociations(r.data));
    client.get("/taxi-ranks").then((r) => setRanks(r.data));
  }

  useEffect(() => {
    reloadAll();
  }, []);

  return (
    <div className="mx-auto max-w-3xl px-4 py-6">
      <p className="mb-1 text-xs font-medium uppercase tracking-wide text-terracotta-600">Platform Administrator</p>
      <h1 className="mb-4 font-display text-xl text-sand-900">App control panel</h1>

      <div className="mb-5 flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`flex items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-sm transition-colors ${
              tab === t.key ? "border-terracotta-600 bg-terracotta-600 text-white" : "border-sand-300 bg-white text-sand-600"
            }`}
          >
            <t.icon size={14} /> {t.label}
          </button>
        ))}
      </div>

      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}

      {tab === "users" && (
        <UsersPanel users={users} associations={associations} ranks={ranks} onChange={reloadAll} onError={setError} />
      )}
      {tab === "vendors" && (
        <VendorsPanel vendors={vendors} associations={associations} ranks={ranks} onChange={reloadAll} onError={setError} />
      )}
      {tab === "associations" && (
        <ReferencePanel
          kind="association"
          items={associations}
          onChange={reloadAll}
          onError={setError}
        />
      )}
      {tab === "ranks" && (
        <ReferencePanel
          kind="rank"
          items={ranks}
          onChange={reloadAll}
          onError={setError}
        />
      )}
    </div>
  );
}

function ConfirmDelete({ onConfirm, label = "this record" }) {
  const [confirming, setConfirming] = useState(false);
  if (!confirming) {
    return (
      <button
        onClick={() => setConfirming(true)}
        className="flex items-center gap-1 rounded-lg border border-red-200 px-2.5 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50"
      >
        <Trash2 size={12} /> Delete
      </button>
    );
  }
  return (
    <div className="flex items-center gap-1.5">
      <span className="text-xs text-sand-500">Delete {label}?</span>
      <button onClick={onConfirm} className="rounded-lg bg-red-600 px-2.5 py-1 text-xs font-semibold text-white">
        Confirm
      </button>
      <button onClick={() => setConfirming(false)} className="text-xs text-sand-500">
        Cancel
      </button>
    </div>
  );
}

function UsersPanel({ users, associations, ranks, onChange, onError }) {
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);

  if (!users) return <SkeletonCard />;

  function startEdit(u) {
    setEditingId(u.id);
    setForm({ name: u.name, surname: u.surname, email: u.email || "", associationId: u.associationId || "", rankId: u.rankId || "" });
  }

  async function save(id) {
    setSaving(true);
    onError("");
    try {
      await client.put(`/admin/users/${id}`, {
        name: form.name,
        surname: form.surname,
        email: form.email || null,
        associationId: form.associationId || null,
        rankId: form.rankId || null,
      });
      setEditingId(null);
      onChange();
    } catch (err) {
      onError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function remove(id) {
    onError("");
    try {
      await client.delete(`/admin/users/${id}`);
      onChange();
    } catch (err) {
      onError(err.message);
    }
  }

  return (
    <div className="space-y-2">
      {users.map((u) => (
        <Card key={u.id}>
          {editingId === u.id ? (
            <div className="space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <Field label="Name">
                  <input className={inputClass} value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
                </Field>
                <Field label="Surname">
                  <input className={inputClass} value={form.surname} onChange={(e) => setForm((f) => ({ ...f, surname: e.target.value }))} />
                </Field>
              </div>
              <Field label="Email">
                <input className={inputClass} value={form.email} onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))} />
              </Field>
              <div className="grid grid-cols-2 gap-2">
                <Field label="Association">
                  <select className={inputClass} value={form.associationId} onChange={(e) => setForm((f) => ({ ...f, associationId: e.target.value }))}>
                    <option value="">—</option>
                    {(associations || []).map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
                  </select>
                </Field>
                <Field label="Rank">
                  <select className={inputClass} value={form.rankId} onChange={(e) => setForm((f) => ({ ...f, rankId: e.target.value }))}>
                    <option value="">—</option>
                    {(ranks || []).map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                  </select>
                </Field>
              </div>
              <div className="flex gap-2">
                <Button loading={saving} onClick={() => save(u.id)}>Save</Button>
                <Button variant="secondary" onClick={() => setEditingId(null)}><X size={14} /> Cancel</Button>
              </div>
            </div>
          ) : (
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-sand-800">{u.name} {u.surname}</p>
                <p className="text-xs text-sand-500">{u.phoneNumber} · {u.userType}</p>
                {u.email && <p className="text-xs text-sand-400">{u.email}</p>}
                {(u.associationName || u.rankName) && (
                  <p className="mt-0.5 text-xs text-sand-400">
                    {[u.associationName, u.rankName].filter(Boolean).join(" · ")}
                  </p>
                )}
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <button onClick={() => startEdit(u)} className="flex items-center gap-1 rounded-lg border border-sand-300 px-2.5 py-1.5 text-xs font-semibold text-sand-700 hover:bg-sand-50">
                  <Pencil size={12} /> Edit
                </button>
                <ConfirmDelete label="this user" onConfirm={() => remove(u.id)} />
              </div>
            </div>
          )}
        </Card>
      ))}
      {users.length === 0 && <p className="text-sm text-sand-400">No users yet.</p>}
    </div>
  );
}

const CATEGORIES = ["TAXI", "FOOD", "SERVICES", "RETAIL", "OTHER"];
const STATUSES = ["PENDING", "APPROVED", "REJECTED"];

function VendorsPanel({ vendors, associations, ranks, onChange, onError }) {
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);

  if (!vendors) return <SkeletonCard />;

  function startEdit(v) {
    setEditingId(v.vendorId);
    setForm({
      businessName: v.businessName,
      category: v.category,
      status: v.status,
      locationName: v.locationName,
      verified: v.verified,
      vehicleRegistration: v.vehicleRegistration || "",
      associationId: v.associationId || "",
      rankId: v.rankId || "",
    });
  }

  async function save(id) {
    setSaving(true);
    onError("");
    try {
      await client.put(`/admin/vendors/${id}`, {
        businessName: form.businessName,
        category: form.category,
        status: form.status,
        locationName: form.locationName,
        verified: form.verified,
        vehicleRegistration: form.vehicleRegistration || null,
        associationId: form.associationId || null,
        rankId: form.rankId || null,
      });
      setEditingId(null);
      onChange();
    } catch (err) {
      onError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function remove(id) {
    onError("");
    try {
      await client.delete(`/admin/vendors/${id}`);
      onChange();
    } catch (err) {
      onError(err.message);
    }
  }

  return (
    <div className="space-y-2">
      {vendors.map((v) => (
        <Card key={v.vendorId}>
          {editingId === v.vendorId ? (
            <div className="space-y-2">
              <Field label="Business name">
                <input className={inputClass} value={form.businessName} onChange={(e) => setForm((f) => ({ ...f, businessName: e.target.value }))} />
              </Field>
              <div className="grid grid-cols-2 gap-2">
                <Field label="Category">
                  <select className={inputClass} value={form.category} onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}>
                    {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                  </select>
                </Field>
                <Field label="Status">
                  <select className={inputClass} value={form.status} onChange={(e) => setForm((f) => ({ ...f, status: e.target.value }))}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </Field>
              </div>
              <Field label="Location">
                <input className={inputClass} value={form.locationName} onChange={(e) => setForm((f) => ({ ...f, locationName: e.target.value }))} />
              </Field>
              <Field label="Vehicle registration">
                <input className={inputClass} value={form.vehicleRegistration} onChange={(e) => setForm((f) => ({ ...f, vehicleRegistration: e.target.value }))} />
              </Field>
              <div className="grid grid-cols-2 gap-2">
                <Field label="Association">
                  <select className={inputClass} value={form.associationId} onChange={(e) => setForm((f) => ({ ...f, associationId: e.target.value }))}>
                    <option value="">—</option>
                    {(associations || []).map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
                  </select>
                </Field>
                <Field label="Rank">
                  <select className={inputClass} value={form.rankId} onChange={(e) => setForm((f) => ({ ...f, rankId: e.target.value }))}>
                    <option value="">—</option>
                    {(ranks || []).map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                  </select>
                </Field>
              </div>
              <label className="flex items-center gap-2 text-sm text-sand-700">
                <input type="checkbox" checked={form.verified} onChange={(e) => setForm((f) => ({ ...f, verified: e.target.checked }))} />
                Verified
              </label>
              <div className="flex gap-2">
                <Button loading={saving} onClick={() => save(v.vendorId)}>Save</Button>
                <Button variant="secondary" onClick={() => setEditingId(null)}><X size={14} /> Cancel</Button>
              </div>
            </div>
          ) : (
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-sand-800">{v.businessName}</p>
                <p className="text-xs text-sand-500">{v.ownerName} · {v.ownerPhone}</p>
                <p className="text-xs text-sand-400">
                  {v.category} · {v.status}{v.verified ? " · verified" : ""}
                </p>
                {(v.associationName || v.rankName) && (
                  <p className="mt-0.5 text-xs text-sand-400">{[v.associationName, v.rankName].filter(Boolean).join(" · ")}</p>
                )}
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <button onClick={() => startEdit(v)} className="flex items-center gap-1 rounded-lg border border-sand-300 px-2.5 py-1.5 text-xs font-semibold text-sand-700 hover:bg-sand-50">
                  <Pencil size={12} /> Edit
                </button>
                <ConfirmDelete label="this vendor profile" onConfirm={() => remove(v.vendorId)} />
              </div>
            </div>
          )}
        </Card>
      ))}
      {vendors.length === 0 && <p className="text-sm text-sand-400">No vendors or drivers yet.</p>}
    </div>
  );
}

function ReferencePanel({ kind, items, onChange, onError }) {
  const isAssociation = kind === "association";
  const basePath = isAssociation ? "/taxi-associations" : "/taxi-ranks";
  const adminPath = isAssociation ? "/admin/taxi-associations" : "/admin/taxi-ranks";

  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ name: "", locationName: "", duesAmount: "" });
  const [newItem, setNewItem] = useState({ name: "", locationName: "" });
  const [saving, setSaving] = useState(false);
  const [adding, setAdding] = useState(false);

  if (!items) return <SkeletonCard />;

  function startEdit(item) {
    setEditingId(item.id);
    setForm({ name: item.name, locationName: item.locationName || "", duesAmount: item.duesAmount ?? "" });
  }

  async function save(id) {
    setSaving(true);
    onError("");
    try {
      await client.put(`${adminPath}/${id}`, {
        name: form.name,
        locationName: form.locationName || null,
        duesAmount: isAssociation && form.duesAmount !== "" ? Number(form.duesAmount) : null,
      });
      setEditingId(null);
      onChange();
    } catch (err) {
      onError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function remove(id) {
    onError("");
    try {
      await client.delete(`${adminPath}/${id}`);
      onChange();
    } catch (err) {
      onError(err.message);
    }
  }

  async function addNew(e) {
    e.preventDefault();
    setAdding(true);
    onError("");
    try {
      await client.post(basePath, { name: newItem.name, locationName: newItem.locationName || null });
      setNewItem({ name: "", locationName: "" });
      onChange();
    } catch (err) {
      onError(err.message);
    } finally {
      setAdding(false);
    }
  }

  return (
    <div className="space-y-3">
      <Card>
        <h2 className="mb-2 text-sm font-semibold text-sand-700">Add a taxi {isAssociation ? "association" : "rank"}</h2>
        <form onSubmit={addNew} className="flex flex-wrap gap-2">
          <input
            required
            placeholder="Name"
            className={`${inputClass} flex-1`}
            value={newItem.name}
            onChange={(e) => setNewItem((f) => ({ ...f, name: e.target.value }))}
          />
          {!isAssociation && (
            <input
              placeholder="Location"
              className={`${inputClass} flex-1`}
              value={newItem.locationName}
              onChange={(e) => setNewItem((f) => ({ ...f, locationName: e.target.value }))}
            />
          )}
          <Button type="submit" loading={adding}>Add</Button>
        </form>
        <p className="mt-1.5 text-xs text-sand-400">
          Reuses the same create endpoint signup uses - typing an existing name (case-insensitive) won't create a duplicate.
        </p>
      </Card>

      <div className="space-y-2">
        {items.map((item) => (
          <Card key={item.id}>
            {editingId === item.id ? (
              <div className="space-y-2">
                <Field label="Name">
                  <input className={inputClass} value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
                </Field>
                {!isAssociation && (
                  <Field label="Location">
                    <input className={inputClass} value={form.locationName} onChange={(e) => setForm((f) => ({ ...f, locationName: e.target.value }))} />
                  </Field>
                )}
                {isAssociation && (
                  <Field label="Membership dues (R)">
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      className={inputClass}
                      value={form.duesAmount}
                      onChange={(e) => setForm((f) => ({ ...f, duesAmount: e.target.value }))}
                    />
                  </Field>
                )}
                <div className="flex gap-2">
                  <Button loading={saving} onClick={() => save(item.id)}>Save</Button>
                  <Button variant="secondary" onClick={() => setEditingId(null)}><X size={14} /> Cancel</Button>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-sand-800">{item.name}</p>
                  {item.locationName && <p className="text-xs text-sand-400">{item.locationName}</p>}
                  {isAssociation && <p className="text-xs text-sand-400">Dues: R{Number(item.duesAmount).toFixed(2)}</p>}
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <button onClick={() => startEdit(item)} className="flex items-center gap-1 rounded-lg border border-sand-300 px-2.5 py-1.5 text-xs font-semibold text-sand-700 hover:bg-sand-50">
                    <Pencil size={12} /> Edit
                  </button>
                  <ConfirmDelete label={isAssociation ? "this association" : "this rank"} onConfirm={() => remove(item.id)} />
                </div>
              </div>
            )}
          </Card>
        ))}
        {items.length === 0 && <p className="text-sm text-sand-400">None yet.</p>}
      </div>
    </div>
  );
}
