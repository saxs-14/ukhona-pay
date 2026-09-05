import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import {
  Smartphone,
  Zap,
  Receipt,
  ArrowLeft,
  CheckCircle2,
  Lock,
  Copy,
  Check,
  AlertCircle,
  Building2,
  ShieldCheck,
  Sparkles,
  RefreshCw,
  Phone,
  FileText
} from "lucide-react";
import client from "../api/client";
import { useAuth } from "../context/AuthContext";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import AnimatedNumber from "../components/ui/AnimatedNumber";
import { SkeletonCard } from "../components/ui/Skeleton";
import NetworkLogo from "../components/ui/NetworkLogos";
import { ease, spring } from "../lib/motion";

// SA Telecom Networks
const NETWORKS = [
  { id: "vodacom", name: "Vodacom", border: "border-red-500", activeBg: "bg-red-50/70", ring: "ring-red-200" },
  { id: "mtn", name: "MTN", border: "border-amber-400", activeBg: "bg-amber-50/70", ring: "ring-amber-200" },
  { id: "telkom", name: "Telkom", border: "border-blue-500", activeBg: "bg-blue-50/70", ring: "ring-blue-200" },
  { id: "cellc", name: "Cell C", border: "border-sand-800", activeBg: "bg-sand-100/70", ring: "ring-sand-300" },
];

const AIRTIME_PRESETS = [10, 20, 30, 50, 100, 200];
const ELECTRICITY_PRESETS = [50, 100, 150, 200, 300, 500];

// Pay@ Biller Categories
const PAYAT_BILLERS = [
  { id: "mbombela", name: "City of Mbombela", category: "Rates & Water", prefix: "11474" },
  { id: "eskom", name: "Eskom Direct", category: "Electricity Account", prefix: "11320" },
  { id: "dstv", name: "DStv / MultiChoice", category: "Subscription", prefix: "11500" },
  { id: "traffic", name: "Mbombela Traffic Dept", category: "Traffic Fines", prefix: "11980" },
  { id: "avbob", name: "AVBOB Mutual Assurance", category: "Funeral Policy", prefix: "11640" },
  { id: "pep", name: "Pep Stores / Ackermans", category: "Retail Account", prefix: "11720" },
];

// Utility vendor for processing driver & vendor service purchases
const UTILITY_VENDOR_QR = "UKP-VENDOR-SIPHOELEC-005";
const FALLBACK_VENDOR_QR = "UKP-VENDOR-NOMSA-006";

export default function BuyServices() {
  const navigate = useNavigate();
  const { user: authUser } = useAuth();
  const [activeTab, setActiveTab] = useState("airtime"); // "airtime" | "electricity" | "payat"
  const [wallet, setWallet] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Form states - Airtime
  const [network, setNetwork] = useState("vodacom");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [airtimeAmount, setAirtimeAmount] = useState("50");

  // Form states - Electricity
  const [meterNumber, setMeterNumber] = useState("");
  const [electricityAmount, setElectricityAmount] = useState("100");
  const [municipality, setMunicipality] = useState("City of Mbombela (Nelspruit / KaNyamazane)");

  // Form states - Pay@
  const [selectedBiller, setSelectedBiller] = useState(PAYAT_BILLERS[0]);
  const [payAtRef, setPayAtRef] = useState("114749281034");
  const [accountName, setAccountName] = useState("");
  const [billAmount, setBillAmount] = useState("250");

  // Common purchase states
  const [pin, setPin] = useState("");
  const [purchasing, setPurchasing] = useState(false);
  const [error, setError] = useState("");
  const [receipt, setReceipt] = useState(null);
  const [copiedCode, setCopiedCode] = useState(false);

  useEffect(() => {
    Promise.all([
      client.get("/wallet/me"),
      client.get("/users/me"),
    ])
      .then(([w, u]) => {
        setWallet(w.data);
        setUser(u.data);
        setRecipientPhone(u.data.phoneNumber || "");
        setAccountName(`${u.data.name} ${u.data.surname || ""}`.trim());
      })
      .finally(() => setLoading(false));
  }, []);

  // Helper to generate simulated STS 20-digit token
  function generateStsToken() {
    const chunk = () => Math.floor(1000 + Math.random() * 9000);
    return `${chunk()} - ${chunk()} - ${chunk()} - ${chunk()} - ${chunk()}`;
  }

  // Helper to generate airtime topup pin
  function generateAirtimePin() {
    return `${Math.floor(1000 + Math.random() * 9000)} ${Math.floor(1000 + Math.random() * 9000)} ${Math.floor(1000 + Math.random() * 9000)}`;
  }

  async function handlePurchase(e) {
    e.preventDefault();
    setError("");
    setPurchasing(true);

    let amountToPay = 0;
    let description = "";
    let receiptDetails = {};

    if (activeTab === "airtime") {
      amountToPay = Number(airtimeAmount);
      if (!recipientPhone || recipientPhone.length < 10) {
        setError("Please enter a valid 10-digit South African cellphone number");
        setPurchasing(false);
        return;
      }
      description = `Airtime: ${network.toUpperCase()} R${amountToPay} (${recipientPhone})`;
      receiptDetails = {
        type: "AIRTIME",
        network: network,
        title: `${network.toUpperCase()} Airtime Recharge`,
        subtitle: `Top-up sent to ${recipientPhone}`,
        token: generateAirtimePin(),
        tokenLabel: "Recharge Voucher PIN",
        dialInstruction: `Dial *130*7467*${Math.floor(100000 + Math.random() * 900000)}# or load directly on your SIM`,
        extraLabel: "Network Provider",
        extraValue: network.toUpperCase(),
      };
    } else if (activeTab === "electricity") {
      amountToPay = Number(electricityAmount);
      const cleanMeter = meterNumber.replace(/\s+/g, "");
      if (cleanMeter.length < 10) {
        setError("Please enter a valid 11-digit prepaid electricity meter number");
        setPurchasing(false);
        return;
      }
      const estimatedKwh = (amountToPay / 2.85).toFixed(1);
      description = `Electricity: Eskom/Mbombela R${amountToPay} (Meter: ${cleanMeter})`;
      receiptDetails = {
        type: "ELECTRICITY",
        title: "Prepaid Electricity Token",
        subtitle: `Meter: ${cleanMeter} • ${municipality}`,
        token: generateStsToken(),
        tokenLabel: "20-Digit STS Keypad Token",
        dialInstruction: `Enter these 20 digits into your in-home CIU keypad, then press # or Enter`,
        extraLabel: "Units Purchased",
        extraValue: `~${estimatedKwh} kWh`,
      };
    } else if (activeTab === "payat") {
      amountToPay = Number(billAmount);
      const cleanRef = payAtRef.replace(/\s+/g, "");
      if (cleanRef.length < 8) {
        setError("Please enter a valid Pay@ reference number (e.g. 11474...)");
        setPurchasing(false);
        return;
      }
      description = `Pay@: ${selectedBiller.name} R${amountToPay} (Ref: ${cleanRef})`;
      receiptDetails = {
        type: "PAYAT",
        title: "Pay@ Bill Payment Clearance",
        subtitle: `${selectedBiller.name} • ${selectedBiller.category}`,
        token: `PA-${cleanRef.slice(-6)}-${Math.floor(1000 + Math.random() * 9000)}`,
        tokenLabel: "Pay@ Clearance Code",
        dialInstruction: "Retain this clearance code as official proof of municipal/retail settlement",
        extraLabel: "Biller Account / Reference",
        extraValue: cleanRef,
      };
    }

    if (amountToPay <= 0) {
      setError("Please enter a valid purchase amount");
      setPurchasing(false);
      return;
    }

    if (amountToPay > (wallet?.balance || 0)) {
      setError(`Insufficient wallet balance. You have R${(wallet?.balance || 0).toFixed(2)}`);
      setPurchasing(false);
      return;
    }

    if (!pin || pin.length !== 4) {
      setError("Please enter your 4-digit security PIN");
      setPurchasing(false);
      return;
    }

    try {
      // Pick a vendor QR that does not belong to the current user
      const targetVendor = (user?.id === 5) ? FALLBACK_VENDOR_QR : UTILITY_VENDOR_QR;

      // Execute payment via backend payment endpoint
      const res = await client.post("/payments/pay", {
        vendorQrCode: targetVendor,
        amount: amountToPay,
        pin: pin,
        description: description,
      });

      // Update wallet balance locally
      setWallet((w) => ({ ...w, balance: res.data.newWalletBalance }));
      
      // Set receipt data
      setReceipt({
        ...receiptDetails,
        amount: amountToPay,
        reference: res.data.reference,
        timestamp: new Date().toLocaleString("en-ZA"),
      });
      setPin("");
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Purchase failed. Please check your PIN and balance.");
    } finally {
      setPurchasing(false);
    }
  }

  function handleCopyToken(text) {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  }

  function handleReset() {
    setReceipt(null);
    setError("");
    setPin("");
  }

  const isVendor = authUser?.userType === "VENDOR" || user?.userType === "VENDOR";
  const homePath = isVendor ? "/vendor" : "/driver";
  const roleBadge = isVendor ? "Merchant Wallet Services" : "Driver Wallet Services";
  const roleSubtitle = isVendor
    ? "Use your merchant wallet balance for instant prepaid airtime, electricity & Pay@ bills."
    : "Use your driver wallet balance for instant prepaid airtime, electricity & Pay@ bills.";

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
      {/* Header */}
      <div className="mb-4 flex items-center justify-between">
        <Link
          to={homePath}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-sand-500 hover:text-terracotta-700 transition-colors"
        >
          <ArrowLeft size={14} /> Back
        </Link>
        <span className="inline-flex items-center gap-1 rounded-full bg-sand-100 px-2.5 py-0.5 text-[11px] font-semibold text-sand-600">
          <ShieldCheck size={12} className="text-bushveld-600" /> {roleBadge}
        </span>
      </div>

      <div className="mb-4">
        <h1 className="font-display text-2xl font-bold text-sand-900">Buy Vouchers &amp; Pay Bills</h1>
        <p className="text-xs text-sand-500 mt-0.5">
          {roleSubtitle}
        </p>
      </div>

      {/* Available Balance Banner */}
      <div className="mb-5 rounded-2xl bg-gradient-to-br from-terracotta-600 to-terracotta-700 p-4 text-white shadow-warm">
        <div className="flex items-center justify-between">
          <span className="text-xs text-terracotta-100 uppercase tracking-wider font-medium">
            Available Wallet Balance
          </span>
          <span className="rounded-full bg-white/20 px-2 py-0.5 text-[10px] font-semibold text-white">
            Instant Deduction
          </span>
        </div>
        <p className="font-display text-2xl font-bold mt-1">
          <AnimatedNumber value={Number(wallet?.balance || 0)} prefix="R" />
        </p>
      </div>

      {/* Service Tabs */}
      {!receipt && (
        <div className="mb-5 grid grid-cols-3 gap-2">
          {[
            { id: "airtime", label: "Airtime", icon: Smartphone },
            { id: "electricity", label: "Electricity", icon: Zap },
            { id: "payat", label: "Pay@ Bills", icon: Receipt },
          ].map((tab) => {
            const isSelected = activeTab === tab.id;
            const Icon = tab.icon;
            return (
              <motion.button
                type="button"
                key={tab.id}
                whileTap={{ scale: 0.95 }}
                transition={spring}
                onClick={() => {
                  setActiveTab(tab.id);
                  setError("");
                }}
                className={`flex flex-col items-center justify-center rounded-2xl border py-3 px-2 transition-all ${
                  isSelected
                    ? "border-terracotta-600 bg-white text-terracotta-700 shadow-sm ring-1 ring-terracotta-500 font-semibold"
                    : "border-sand-200 bg-white/70 text-sand-600 hover:bg-white hover:border-sand-300"
                }`}
              >
                <Icon size={18} className={isSelected ? "text-terracotta-600" : "text-sand-400"} />
                <span className="mt-1 text-xs">{tab.label}</span>
              </motion.button>
            );
          })}
        </div>
      )}

      {/* Main Form Card */}
      {!receipt ? (
        <Card className="p-5">
          <form onSubmit={handlePurchase} className="space-y-4">
            {/* 1. AIRTIME TAB */}
            {activeTab === "airtime" && (
              <div className="space-y-4">
                <div>
                  <label className="mb-2 block text-xs font-semibold text-sand-700">Select Mobile Network</label>
                  <div className="grid grid-cols-4 gap-2">
                    {NETWORKS.map((net) => {
                      const isSelected = network === net.id;
                      return (
                        <button
                          type="button"
                          key={net.id}
                          onClick={() => setNetwork(net.id)}
                          className={`flex flex-col items-center justify-center rounded-2xl border py-2.5 px-1 text-xs transition-all ${
                            isSelected
                              ? `${net.border} ${net.activeBg} font-bold text-sand-900 shadow-sm ring-2 ${net.ring}`
                              : "border-sand-200 bg-white text-sand-600 hover:border-sand-300 hover:bg-sand-50/50"
                          }`}
                        >
                          <div className="mb-1 flex h-8 w-8 items-center justify-center">
                            <NetworkLogo network={net.id} className="h-7 w-7 drop-shadow-sm transition-transform hover:scale-105" />
                          </div>
                          <span className="text-[11px] font-semibold">{net.name}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-sand-700">Recipient Phone Number</label>
                  <div className="relative">
                    <Phone size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sand-400" />
                    <input
                      type="tel"
                      required
                      placeholder="0798765432"
                      value={recipientPhone}
                      onChange={(e) => setRecipientPhone(e.target.value)}
                      className="w-full rounded-xl border border-sand-300 bg-sand-50/50 py-2.5 pl-10 pr-3 text-xs font-mono text-sand-900 focus:border-terracotta-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                    />
                  </div>
                  <p className="mt-1 text-[10px] text-sand-400">Defaulted to your profile phone number</p>
                </div>

                <div>
                  <label className="mb-2 block text-xs font-semibold text-sand-700">Select Voucher Amount</label>
                  <div className="grid grid-cols-3 gap-2 mb-2">
                    {AIRTIME_PRESETS.map((amt) => {
                      const isSelected = airtimeAmount === String(amt);
                      return (
                        <button
                          type="button"
                          key={amt}
                          onClick={() => setAirtimeAmount(String(amt))}
                          className={`rounded-xl border py-2 text-xs font-bold transition-all ${
                            isSelected
                              ? "border-terracotta-600 bg-terracotta-50 text-terracotta-800"
                              : "border-sand-200 bg-white text-sand-700 hover:border-sand-300"
                          }`}
                        >
                          R{amt}
                        </button>
                      );
                    })}
                  </div>
                  <div className="relative mt-2">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-sand-400">R</span>
                    <input
                      type="number"
                      min="5"
                      max="1000"
                      placeholder="Or enter custom amount (R5 - R1000)"
                      value={airtimeAmount}
                      onChange={(e) => setAirtimeAmount(e.target.value)}
                      className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-8 pr-3 text-xs text-sand-800 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* 2. ELECTRICITY TAB */}
            {activeTab === "electricity" && (
              <div className="space-y-4">
                <div>
                  <label className="mb-1 block text-xs font-semibold text-sand-700">Electricity Municipality / Provider</label>
                  <select
                    value={municipality}
                    onChange={(e) => setMunicipality(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2.5 px-3 text-xs text-sand-800 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                  >
                    <option value="City of Mbombela (Nelspruit / KaNyamazane)">City of Mbombela (Nelspruit / KaNyamazane)</option>
                    <option value="Eskom Direct Prepaid">Eskom Direct Prepaid</option>
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-sand-700">Prepaid Meter Number (11 Digits)</label>
                  <div className="relative">
                    <Zap size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sand-400" />
                    <input
                      type="text"
                      required
                      placeholder="e.g. 04 1234 5678 9"
                      value={meterNumber}
                      onChange={(e) => setMeterNumber(e.target.value)}
                      className="w-full rounded-xl border border-sand-300 bg-white py-2.5 pl-10 pr-3 text-xs font-mono tracking-wider text-sand-900 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                    />
                  </div>
                  <p className="mt-1 text-[10px] text-sand-400">Found on your CIU keypad or previous slip</p>
                </div>

                <div>
                  <label className="mb-2 block text-xs font-semibold text-sand-700">Select Electricity Amount</label>
                  <div className="grid grid-cols-3 gap-2 mb-2">
                    {ELECTRICITY_PRESETS.map((amt) => {
                      const isSelected = electricityAmount === String(amt);
                      const approxKwh = (amt / 2.85).toFixed(0);
                      return (
                        <button
                          type="button"
                          key={amt}
                          onClick={() => setElectricityAmount(String(amt))}
                          className={`flex flex-col items-center justify-center rounded-xl border py-2 text-xs transition-all ${
                            isSelected
                              ? "border-terracotta-600 bg-terracotta-50 text-terracotta-800 font-bold"
                              : "border-sand-200 bg-white text-sand-700 hover:border-sand-300"
                          }`}
                        >
                          <span className="font-bold">R{amt}</span>
                          <span className="text-[10px] text-sand-500">~{approxKwh} kWh</span>
                        </button>
                      );
                    })}
                  </div>
                  <div className="relative mt-2">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-sand-400">R</span>
                    <input
                      type="number"
                      min="20"
                      max="2000"
                      placeholder="Or enter custom amount (min R20)"
                      value={electricityAmount}
                      onChange={(e) => setElectricityAmount(e.target.value)}
                      className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-8 pr-3 text-xs text-sand-800 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* 3. PAY@ BILLS TAB */}
            {activeTab === "payat" && (
              <div className="space-y-4">
                <div>
                  <label className="mb-2 block text-xs font-semibold text-sand-700">Select Biller or Service</label>
                  <div className="grid grid-cols-2 gap-2">
                    {PAYAT_BILLERS.map((biller) => {
                      const isSelected = selectedBiller.id === biller.id;
                      return (
                        <button
                          type="button"
                          key={biller.id}
                          onClick={() => {
                            setSelectedBiller(biller);
                            setPayAtRef(`${biller.prefix}${Math.floor(100000 + Math.random() * 900000)}`);
                          }}
                          className={`flex flex-col items-start rounded-xl border p-2.5 text-left text-xs transition-all ${
                            isSelected
                              ? "border-terracotta-600 bg-terracotta-50/70 text-terracotta-900 ring-1 ring-terracotta-500"
                              : "border-sand-200 bg-white text-sand-700 hover:border-sand-300"
                          }`}
                        >
                          <span className="font-bold text-[11px] leading-tight">{biller.name}</span>
                          <span className="text-[10px] text-sand-500 mt-0.5">{biller.category}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-sand-700">Pay@ Account / Reference Number</label>
                  <div className="relative">
                    <Receipt size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sand-400" />
                    <input
                      type="text"
                      required
                      placeholder="e.g. 11474 9281 034"
                      value={payAtRef}
                      onChange={(e) => setPayAtRef(e.target.value)}
                      className="w-full rounded-xl border border-sand-300 bg-white py-2.5 pl-10 pr-3 text-xs font-mono tracking-wider text-sand-900 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                    />
                  </div>
                  <p className="mt-1 text-[10px] text-sand-400">Begins with Pay@ prefix {selectedBiller.prefix}</p>
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-sand-700">Account Holder Name</label>
                  <input
                    type="text"
                    value={accountName}
                    onChange={(e) => setAccountName(e.target.value)}
                    placeholder="Full name as on bill"
                    className="w-full rounded-xl border border-sand-300 bg-white py-2 px-3 text-xs text-sand-900 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-sand-700">Payment Amount (ZAR)</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-sand-400">R</span>
                    <input
                      type="number"
                      min="10"
                      required
                      placeholder="Amount to pay on bill"
                      value={billAmount}
                      onChange={(e) => setBillAmount(e.target.value)}
                      className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-8 pr-3 text-xs text-sand-800 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Common Security PIN */}
            <div className="border-t border-sand-200 pt-3">
              <label className="mb-1 block text-xs font-semibold text-sand-700">Enter Your 4-Digit Security PIN</label>
              <div className="relative">
                <Lock size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sand-400" />
                <input
                  type="password"
                  inputMode="numeric"
                  maxLength={4}
                  required
                  placeholder="••••"
                  value={pin}
                  onChange={(e) => setPin(e.target.value.replace(/\D/g, ""))}
                  className="w-full rounded-xl border border-sand-300 bg-white py-2 pl-10 pr-3 text-center font-bold tracking-[0.4em] text-sm text-sand-900 focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
                />
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 rounded-xl bg-red-50 p-2.5 text-xs text-red-600">
                <AlertCircle size={15} className="shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <Button
              type="submit"
              loading={purchasing}
              disabled={purchasing || (wallet?.balance || 0) <= 0}
              className="w-full py-3 text-xs font-semibold shadow-warm"
            >
              {activeTab === "airtime" && `Buy R${airtimeAmount || "0"} Airtime`}
              {activeTab === "electricity" && `Buy R${electricityAmount || "0"} Electricity`}
              {activeTab === "payat" && `Pay R${billAmount || "0"} via Pay@`}
            </Button>
          </form>
        </Card>
      ) : (
        /* Instant Result / Voucher Receipt */
        <motion.div
          initial={{ opacity: 0, scale: 0.96 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.35, ease: ease.enter }}
          className="rounded-2xl border border-sand-200 bg-white p-6 text-center shadow-warm"
        >
          <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-bushveld-100 text-bushveld-600 shadow-sm">
            <CheckCircle2 size={32} />
          </div>

          <span className="rounded-full bg-bushveld-50 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-bushveld-700">
            Purchase Successful
          </span>

          <h2 className="font-display text-2xl font-bold text-sand-900 mt-2">
            R{Number(receipt.amount).toFixed(2)}
          </h2>
          <p className="text-xs text-sand-500">{receipt.title}</p>
          <p className="text-[11px] text-sand-400 mt-0.5">{receipt.subtitle}</p>

          {/* Token Box */}
          <div className="mt-5 rounded-2xl border border-dashed border-sand-300 bg-sand-50/90 p-4 text-left">
            <div className="flex items-center justify-between text-xs text-sand-500 mb-1.5">
              <span className="font-semibold text-sand-700">{receipt.tokenLabel}:</span>
              <button
                type="button"
                onClick={() => handleCopyToken(receipt.token)}
                className="flex items-center gap-1 text-[11px] font-bold text-terracotta-700 hover:underline"
              >
                {copiedCode ? <Check size={12} className="text-bushveld-600" /> : <Copy size={12} />}
                {copiedCode ? "Copied!" : "Copy"}
              </button>
            </div>

            <p className="font-mono text-base font-bold text-sand-900 tracking-wider py-1 bg-white rounded-lg border border-sand-200 px-3 text-center">
              {receipt.token}
            </p>

            <p className="mt-2 text-[11px] text-sand-500 leading-relaxed">
              {receipt.dialInstruction}
            </p>

            <div className="mt-3 border-t border-sand-200 pt-2 flex items-center justify-between text-xs text-sand-500">
              <span>{receipt.extraLabel}:</span>
              <span className="font-bold text-sand-800 flex items-center gap-1.5">
                {receipt.network && <NetworkLogo network={receipt.network} className="h-4 w-4 inline-block" />}
                {receipt.extraValue}
              </span>
            </div>
            <div className="flex items-center justify-between text-xs text-sand-500 mt-1">
              <span>Payment Ref:</span>
              <span className="font-mono text-[11px] text-sand-700">{receipt.reference}</span>
            </div>
          </div>

          <div className="mt-5 flex gap-2">
            <Button
              variant="outline"
              onClick={() => handleCopyToken(receipt.token)}
              className="flex-1 text-xs"
            >
              <Copy size={13} /> {copiedCode ? "Copied!" : "Copy Voucher"}
            </Button>
            <Button
              onClick={handleReset}
              className="flex-1 text-xs"
            >
              Buy Another
            </Button>
          </div>

          <Link
            to="/transactions"
            className="mt-3 inline-block text-xs font-semibold text-sand-500 hover:text-terracotta-700"
          >
            View in Transaction Ledger →
          </Link>
        </motion.div>
      )}
    </div>
  );
}
