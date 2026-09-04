import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { BadgeCheck, Banknote, CheckCircle2, Lock, ScanLine, X } from "lucide-react";
import client from "../api/client";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";

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

const READER_ID = "driver-qr-reader";

export default function DriverScanPay() {
  const scannerRef = useRef(null);
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState("");
  const [manualCode, setManualCode] = useState("");

  const [vendor, setVendor] = useState(null);
  const [lookupError, setLookupError] = useState("");

  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [pin, setPin] = useState("");
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState("");
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    return () => {
      stopCamera();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function startCamera() {
    setCameraError("");
    try {
      const { Html5Qrcode } = await import("html5-qrcode");
      const scanner = new Html5Qrcode(READER_ID);
      scannerRef.current = scanner;
      await scanner.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: 220 },
        (decodedText) => {
          handleScanned(decodedText);
        },
        () => {
          // per-frame "no QR found" callback - expected constantly while scanning, ignore
        }
      );
      setCameraActive(true);
    } catch {
      setCameraError(
        "Couldn't access the camera (needs HTTPS or localhost, and camera permission). Use manual entry below instead."
      );
    }
  }

  function stopCamera() {
    const scanner = scannerRef.current;
    if (scanner) {
      scanner.stop().then(() => scanner.clear()).catch(() => {});
      scannerRef.current = null;
    }
    setCameraActive(false);
  }

  async function handleScanned(qrCode) {
    stopCamera();
    await lookupVendor(qrCode);
  }

  async function lookupVendor(qrCode) {
    setLookupError("");
    try {
      const { data } = await client.get(`/vendors/qr/${encodeURIComponent(qrCode)}`);
      setVendor(data);
    } catch (err) {
      setLookupError(err.message);
    }
  }

  function handleManualLookup(e) {
    e.preventDefault();
    if (manualCode.trim()) lookupVendor(manualCode.trim());
  }

  function reset() {
    setVendor(null);
    setAmount("");
    setDescription("");
    setPin("");
    setPayError("");
    setSuccess(null);
    setManualCode("");
  }

  async function handlePay(e) {
    e.preventDefault();
    setPayError("");
    setSuccess(null);
    setPaying(true);
    try {
      const { data } = await client.post("/payments/pay", {
        vendorQrCode: vendor.qrCode,
        amount: Number(amount),
        pin,
        description: description || null,
      });
      setSuccess(data);
    } catch (err) {
      setPayError(err.message);
    } finally {
      setPaying(false);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <ScanLine size={20} className="text-terracotta-600" /> Scan &amp; pay
      </h1>
      <p className="mb-4 text-sm text-sand-500">Scan a vendor's QR code to pay them from your wallet.</p>

      {!vendor && (
        <>
          <Card>
            <div id={READER_ID} className={cameraActive ? "overflow-hidden rounded-xl" : "hidden"} />
            {!cameraActive && (
              <div className="flex flex-col items-center gap-3 py-6 text-center">
                <ScanLine size={32} className="text-terracotta-400" />
                <Button onClick={startCamera}>Start camera</Button>
                {cameraError && <p className="max-w-xs text-xs text-red-600">{cameraError}</p>}
              </div>
            )}
            {cameraActive && (
              <Button variant="secondary" className="mt-3 w-full" onClick={stopCamera}>
                <X size={14} /> Stop camera
              </Button>
            )}
          </Card>

          <Card className="mt-3">
            <h2 className="mb-2 text-sm font-semibold text-sand-700">Or enter the code manually</h2>
            <form onSubmit={handleManualLookup} className="flex gap-2">
              <input
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
                placeholder="UKP-VENDOR-..."
                className={inputClass}
              />
              <Button type="submit">Find</Button>
            </form>
            {lookupError && <p className="mt-2 text-sm text-red-600">{lookupError}</p>}
          </Card>
        </>
      )}

      {vendor && !success && (
        <Card className="mt-3">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <p className="flex items-center gap-1.5 text-sm font-semibold text-sand-800">
                {vendor.businessName} {vendor.verified && <BadgeCheck size={14} className="text-terracotta-600" />}
              </p>
              <p className="text-xs text-sand-400">{vendor.locationName}</p>
            </div>
            <button type="button" onClick={reset} className="text-xs font-semibold text-terracotta-700">
              Change
            </button>
          </div>
          <form onSubmit={handlePay} className="space-y-3">
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
            <Field label="Note (optional)">
              <input value={description} onChange={(e) => setDescription(e.target.value)} className={inputClass} />
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
            {payError && <p className="text-sm text-red-600">{payError}</p>}
            <Button type="submit" loading={paying} className="w-full">
              Pay R{amount || "0.00"}
            </Button>
          </form>
        </Card>
      )}

      {success && (
        <Card className="mt-3">
          <motion.div initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} className="text-center">
            <CheckCircle2 size={32} className="mx-auto mb-2 text-bushveld-600" />
            <p className="font-semibold text-sand-800">Paid R{Number(success.amount).toFixed(2)} to {success.vendorName}</p>
            <p className="mt-1 text-xs text-sand-400">ref {success.reference}</p>
            <p className="mt-3 text-sm text-sand-600">New balance: R{Number(success.newWalletBalance).toFixed(2)}</p>
            <Button className="mt-4 w-full" onClick={reset}>
              Scan another
            </Button>
          </motion.div>
        </Card>
      )}
    </div>
  );
}
