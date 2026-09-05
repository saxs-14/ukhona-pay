import { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, CheckCircle2, AlertCircle, Camera, Lock, RefreshCw } from "lucide-react";
import { Html5Qrcode } from "html5-qrcode";
import client from "../api/client";

export default function ScanAndPayModal({ isOpen, onClose, walletBalance, onSuccess }) {
  const [step, setStep] = useState("SCAN"); // "SCAN", "AMOUNT", "SUCCESS"
  const [scannedCode, setScannedCode] = useState("");
  const [recipient, setRecipient] = useState(null);
  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");
  const [loading, setLoading] = useState(false);
  const [cameraActive, setCameraActive] = useState(false);
  const [error, setError] = useState("");
  const html5QrCodeRef = useRef(null);

  const startCamera = async () => {
    setError("");
    setCameraActive(false);
    
    // Give DOM time to mount #qr-reader
    await new Promise((resolve) => setTimeout(resolve, 150));
    const element = document.getElementById("qr-reader");
    if (!element) return;

    try {
      if (html5QrCodeRef.current) {
        await html5QrCodeRef.current.stop().catch(() => {});
        html5QrCodeRef.current = null;
      }

      const instance = new Html5Qrcode("qr-reader");
      html5QrCodeRef.current = instance;

      await instance.start(
        { facingMode: "environment" },
        {
          fps: 10,
          qrbox: { width: 220, height: 220 },
        },
        (decodedText) => {
          instance.stop().then(() => {
            html5QrCodeRef.current = null;
            setCameraActive(false);
          }).catch(() => {});
          handleCodeScanned(decodedText);
        },
        () => {}
      );
      setCameraActive(true);
    } catch (err) {
      console.warn("Direct camera start failed, trying default camera", err);
      // Fallback to user facing camera if environment camera fails
      try {
        if (html5QrCodeRef.current) {
          await html5QrCodeRef.current.start(
            { facingMode: "user" },
            { fps: 10, qrbox: { width: 220, height: 220 } },
            (decodedText) => {
              html5QrCodeRef.current.stop().catch(() => {});
              handleCodeScanned(decodedText);
            },
            () => {}
          );
          setCameraActive(true);
        }
      } catch (e) {
        setError("Camera permission denied or camera not accessible. Please enter QR code manually below.");
        setCameraActive(false);
      }
    }
  };

  const stopCamera = async () => {
    if (html5QrCodeRef.current) {
      try {
        await html5QrCodeRef.current.stop();
      } catch (e) {}
      html5QrCodeRef.current = null;
    }
    setCameraActive(false);
  };

  useEffect(() => {
    if (isOpen && step === "SCAN") {
      startCamera();
    } else {
      stopCamera();
    }

    return () => {
      stopCamera();
    };
  }, [isOpen, step]);

  const handleCodeScanned = async (code) => {
    setScannedCode(code);
    setError("");
    setLoading(true);
    try {
      const res = await client.get(`/vendors/qr/${encodeURIComponent(code)}`);
      setRecipient(res.data);
      setStep("AMOUNT");
    } catch (err) {
      setError("Vendor not found for this QR code. Please verify and try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleManualLookup = (e) => {
    e.preventDefault();
    if (!scannedCode.trim()) return;
    stopCamera();
    handleCodeScanned(scannedCode.trim());
  };

  const handlePay = async (e) => {
    e.preventDefault();
    setError("");

    const numAmount = Number(amount);
    if (!numAmount || numAmount <= 0) {
      setError("Please enter a valid amount");
      return;
    }
    if (numAmount > walletBalance) {
      setError(`Insufficient wallet balance. Available: R${walletBalance.toFixed(2)}`);
      return;
    }
    if (!pin || pin.length !== 4) {
      setError("PIN must be exactly 4 digits");
      return;
    }

    setLoading(true);
    try {
      await client.post("/payments/pay", {
        vendorQrCode: scannedCode,
        amount: numAmount,
        pin: pin,
        description: "Vendor QR Payment",
      });
      setStep("SUCCESS");
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || "Payment failed. Please check your PIN and balance.");
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    stopCamera();
    setStep("SCAN");
    setScannedCode("");
    setRecipient(null);
    setAmount("");
    setPin("");
    setError("");
    onClose();
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-sand-900/60 p-4 backdrop-blur-sm">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="relative w-full max-w-sm rounded-3xl border border-sand-200 bg-white p-6 shadow-2xl"
        >
          <button
            onClick={handleClose}
            className="absolute right-4 top-4 rounded-full p-2 text-sand-400 hover:bg-sand-100 hover:text-sand-700"
          >
            <X size={20} />
          </button>

          {step === "SCAN" && (
            <div>
              <div className="mb-4 text-center">
                <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-2xl bg-terracotta-50 text-terracotta-600">
                  <Camera size={24} />
                </div>
                <h3 className="font-display text-lg text-sand-900">Scan QR Code to Pay</h3>
                <p className="text-xs text-sand-500">Point camera at recipient's QR code</p>
              </div>

              {/* Direct Camera Viewfinder Container */}
              <div className="relative min-h-[240px] overflow-hidden rounded-2xl border border-sand-200 bg-sand-900">
                <div id="qr-reader" className="w-full" />
                {!cameraActive && !error && (
                  <div className="absolute inset-0 flex flex-col items-center justify-center text-white">
                    <RefreshCw size={24} className="mb-2 animate-spin text-terracotta-400" />
                    <p className="text-xs">Starting camera...</p>
                  </div>
                )}
              </div>

              <div className="my-4 text-center text-xs font-semibold uppercase tracking-wider text-sand-400">
                or enter code manually
              </div>

              <form onSubmit={handleManualLookup} className="space-y-3">
                <input
                  type="text"
                  placeholder="e.g. UKP-VENDOR-1001-123456"
                  value={scannedCode}
                  onChange={(e) => setScannedCode(e.target.value)}
                  className="w-full rounded-xl border border-sand-300 px-4 py-2.5 text-sm text-sand-900 placeholder:text-sand-400 focus:border-terracotta-600 focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={loading || !scannedCode.trim()}
                  className="w-full rounded-xl bg-terracotta-600 py-3 text-sm font-semibold text-white transition hover:bg-terracotta-700 disabled:opacity-50"
                >
                  {loading ? "Searching..." : "Continue"}
                </button>
              </form>

              {error && (
                <div className="mt-3 flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs text-rose-700">
                  <AlertCircle size={16} className="shrink-0" />
                  <span>{error}</span>
                </div>
              )}
            </div>
          )}

          {step === "AMOUNT" && recipient && (
            <form onSubmit={handlePay} className="space-y-4">
              <div className="text-center">
                <p className="text-xs font-semibold text-terracotta-600">Paying To</p>
                <h3 className="font-display text-lg font-bold text-sand-900">{recipient.businessName}</h3>
                <p className="text-xs text-sand-500">{recipient.locationName}</p>
              </div>

              <div className="rounded-2xl border border-sand-200 bg-sand-50 p-4">
                <label className="mb-1 block text-xs font-medium text-sand-600">Amount (ZAR)</label>
                <div className="relative flex items-center">
                  <span className="absolute left-3 text-lg font-bold text-sand-400">R</span>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2.5 pl-8 pr-4 text-lg font-semibold text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
                <p className="mt-1.5 text-xs text-sand-500">
                  Available wallet balance: <span className="font-semibold text-bushveld-700">R{walletBalance?.toFixed(2)}</span>
                </p>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-sand-600">Enter Your 4-Digit PIN</label>
                <div className="relative flex items-center">
                  <Lock size={16} className="absolute left-3 text-sand-400" />
                  <input
                    type="password"
                    maxLength={4}
                    placeholder="••••"
                    value={pin}
                    onChange={(e) => setPin(e.target.value)}
                    className="w-full rounded-xl border border-sand-300 bg-white py-2.5 pl-9 pr-4 text-center text-lg font-bold tracking-widest text-sand-900 focus:border-terracotta-600 focus:outline-none"
                    required
                  />
                </div>
              </div>

              {error && (
                <div className="flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs text-rose-700">
                  <AlertCircle size={16} className="shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setStep("SCAN")}
                  className="w-1/3 rounded-xl border border-sand-300 py-3 text-sm font-semibold text-sand-700 hover:bg-sand-50"
                >
                  Back
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-2/3 rounded-xl bg-terracotta-600 py-3 text-sm font-semibold text-white transition hover:bg-terracotta-700 disabled:opacity-50"
                >
                  {loading ? "Processing..." : `Pay R${amount || "0.00"}`}
                </button>
              </div>
            </form>
          )}

          {step === "SUCCESS" && (
            <div className="py-6 text-center">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", stiffness: 260, damping: 20 }}
                className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-bushveld-100 text-bushveld-600"
              >
                <CheckCircle2 size={40} />
              </motion.div>
              <h3 className="font-display text-xl text-sand-900">Payment Successful!</h3>
              <p className="mt-1 text-sm text-sand-600">
                You paid <span className="font-bold text-bushveld-700">R{Number(amount).toFixed(2)}</span> to{" "}
                <span className="font-medium text-sand-900">{recipient?.businessName}</span>
              </p>
              <button
                onClick={handleClose}
                className="mt-6 w-full rounded-xl bg-terracotta-600 py-3 text-sm font-semibold text-white transition hover:bg-terracotta-700"
              >
                Done
              </button>
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
