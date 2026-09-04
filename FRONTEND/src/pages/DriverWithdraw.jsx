import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Banknote, CheckCircle2, Landmark, Lock } from "lucide-react";
import client from "../api/client";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import { listContainer, listItem } from "../lib/motion";

const inputClass =
  "w-full rounded-xl border border-sand-300 bg-sand-50/50 px-3.5 py-2.5 text-sand-900 transition-colors focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100";

function Field({ label, children }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-sand-700">{label}</label>
      {children}
    </div>
  );
}

export default function DriverWithdraw() {
  const [wallet, setWallet] = useState(null);
  const [bankAccount, setBankAccount] = useState(undefined); // undefined = loading, null = none saved
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingAccount, setEditingAccount] = useState(false);

  const [accountForm, setAccountForm] = useState({ accountHolderName: "", bankName: "", accountNumber: "", branchCode: "" });
  const [savingAccount, setSavingAccount] = useState(false);
  const [accountError, setAccountError] = useState("");

  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const [withdrawError, setWithdrawError] = useState("");
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    Promise.all([
      client.get("/wallet/me"),
      client.get("/bank-accounts/me").then((r) => r.data).catch(() => null),
      client.get("/bank-withdrawals/me").then((r) => r.data).catch(() => []),
    ])
      .then(([w, account, h]) => {
        setWallet(w.data);
        setBankAccount(account);
        setHistory(h);
      })
      .finally(() => setLoading(false));
  }

  async function handleSaveAccount(e) {
    e.preventDefault();
    setAccountError("");
    setSavingAccount(true);
    try {
      const { data } = await client.put("/bank-accounts/me", accountForm);
      setBankAccount(data);
      setEditingAccount(false);
      setAccountForm({ accountHolderName: "", bankName: "", accountNumber: "", branchCode: "" });
    } catch (err) {
      setAccountError(err.message);
    } finally {
      setSavingAccount(false);
    }
  }

  async function handleWithdraw(e) {
    e.preventDefault();
    setWithdrawError("");
    setSuccess(null);
    setWithdrawing(true);
    try {
      const { data } = await client.post("/bank-withdrawals", { amount: Number(amount), pin });
      setSuccess(data);
      setWallet((w) => ({ ...w, balance: data.newWalletBalance }));
      setHistory((h) => [data, ...h]);
      setAmount("");
      setPin("");
    } catch (err) {
      setWithdrawError(err.message);
    } finally {
      setWithdrawing(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  const showAccountForm = !bankAccount || editingAccount;

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <Landmark size={20} className="text-terracotta-600" /> Withdraw to bank
      </h1>
      <p className="mb-4 text-sm text-sand-500">Move money from your Ukhona Pay wallet to your own bank account.</p>

      <div className="rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-4 text-white shadow-warm">
        <p className="text-xs text-terracotta-100">Available balance</p>
        <p className="text-2xl font-semibold">
          <AnimatedNumber value={Number(wallet.balance)} prefix="R" />
        </p>
      </div>

      {showAccountForm ? (
        <Card className="mt-5">
          <h2 className="mb-3 text-sm font-semibold text-sand-700">
            {bankAccount ? "Update your bank account" : "Add your bank account"}
          </h2>
          <form onSubmit={handleSaveAccount} className="space-y-3">
            <Field label="Account holder name">
              <input
                required
                value={accountForm.accountHolderName}
                onChange={(e) => setAccountForm((f) => ({ ...f, accountHolderName: e.target.value }))}
                className={inputClass}
              />
            </Field>
            <Field label="Bank name">
              <input
                required
                value={accountForm.bankName}
                onChange={(e) => setAccountForm((f) => ({ ...f, bankName: e.target.value }))}
                className={inputClass}
              />
            </Field>
            <Field label="Account number">
              <input
                required
                inputMode="numeric"
                value={accountForm.accountNumber}
                onChange={(e) => setAccountForm((f) => ({ ...f, accountNumber: e.target.value.replace(/\D/g, "") }))}
                className={inputClass}
              />
            </Field>
            <Field label="Branch code">
              <input
                required
                inputMode="numeric"
                value={accountForm.branchCode}
                onChange={(e) => setAccountForm((f) => ({ ...f, branchCode: e.target.value.replace(/\D/g, "") }))}
                className={inputClass}
              />
            </Field>
            {accountError && <p className="text-sm text-red-600">{accountError}</p>}
            <div className="flex gap-2">
              <Button type="submit" loading={savingAccount} className="flex-1">
                Save account
              </Button>
              {bankAccount && (
                <Button type="button" variant="secondary" onClick={() => setEditingAccount(false)}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </Card>
      ) : (
        <>
          <Card className="mt-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-sand-800">{bankAccount.bankName}</p>
                <p className="text-xs text-sand-400">
                  {bankAccount.accountHolderName} · {bankAccount.maskedAccountNumber}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setEditingAccount(true)}
                className="text-xs font-semibold text-terracotta-700"
              >
                Change
              </button>
            </div>
          </Card>

          <Card className="mt-3">
            <form onSubmit={handleWithdraw} className="space-y-3">
              <Field label="Amount (R)">
                <div className="relative">
                  <Banknote size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
                  <input
                    required
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className={`${inputClass} pl-10`}
                  />
                </div>
              </Field>
              <Field label="PIN">
                <div className="relative">
                  <Lock size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" />
                  <input
                    required
                    type="password"
                    inputMode="numeric"
                    maxLength={4}
                    value={pin}
                    onChange={(e) => setPin(e.target.value.replace(/\D/g, ""))}
                    className={`${inputClass} pl-10 tracking-[0.4em]`}
                  />
                </div>
              </Field>
              {withdrawError && <p className="text-sm text-red-600">{withdrawError}</p>}
              {success && (
                <motion.p
                  initial={{ opacity: 0, y: -4 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex items-center gap-1.5 text-sm text-bushveld-600"
                >
                  <CheckCircle2 size={14} /> Withdrawal complete · ref {success.reference}
                </motion.p>
              )}
              <Button type="submit" loading={withdrawing} className="w-full">
                Withdraw
              </Button>
            </form>
          </Card>
        </>
      )}

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-semibold text-sand-700">Recent bank withdrawals</h2>
        <motion.div variants={listContainer} initial="initial" animate="animate" className="space-y-2">
          {history.map((h) => (
            <motion.div key={h.reference} variants={listItem} className="flex items-center justify-between rounded-xl border border-sand-200 bg-white px-4 py-3">
              <div>
                <p className="text-sm font-medium text-sand-800">{h.bankName}{h.maskedAccountNumber ? ` · ${h.maskedAccountNumber}` : ""}</p>
                <p className="text-xs text-sand-400">{new Date(h.createdAt).toLocaleString("en-ZA")}</p>
              </div>
              <p className="text-sm font-semibold text-sand-800">-R{Number(h.amount).toFixed(2)}</p>
            </motion.div>
          ))}
          {history.length === 0 && <p className="text-sm text-sand-400">No bank withdrawals yet.</p>}
        </motion.div>
      </div>
    </div>
  );
}
