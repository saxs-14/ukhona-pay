import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Camera } from "lucide-react";
import { Html5Qrcode } from "html5-qrcode";
import Button from "../components/ui/Button";

const SCANNER_ID = "ukp-qr-reader";

export default function ScanQr() {
  const navigate = useNavigate();
  const scannerRef = useRef(null);
  const [error, setError] = useState("");
  const [manualCode, setManualCode] = useState("");
  const [starting, setStarting] = useState(true);

  useEffect(() => {
    const scanner = new Html5Qrcode(SCANNER_ID);
    scannerRef.current = scanner;
    let stopped = false;

    scanner
      .start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 240, height: 240 } },
        (decodedText) => {
          if (stopped) return;
          stopped = true;
          scanner.stop().finally(() => navigate(`/vendors/${decodedText}`));
        },
        () => {} // ignore per-frame decode failures
      )
      .then(() => setStarting(false))
      .catch(() => {
        setStarting(false);
        setError("Couldn't access the camera. You can enter the vendor's QR code manually below.");
      });

    return () => {
      if (!stopped) {
        scanner.stop().catch(() => {});
      }
    };
  }, [navigate]);

  function handleManualSubmit(e) {
    e.preventDefault();
    if (manualCode.trim()) {
      navigate(`/vendors/${manualCode.trim()}`);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <Camera size={20} className="text-terracotta-600" /> Scan to pay
      </h1>
      <p className="mb-4 text-sm text-sand-500">Point your camera at the vendor's Ukhona Pay QR code</p>

      <motion.div
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative overflow-hidden rounded-2xl border border-sand-200 bg-black shadow-warm"
      >
        <div id={SCANNER_ID} className="mx-auto w-full" />
        {starting && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/40 text-sm text-white">
            Starting camera...
          </div>
        )}
      </motion.div>
      {error && <p className="mt-2 text-sm text-gold-600">{error}</p>}

      <form onSubmit={handleManualSubmit} className="mt-6 flex gap-2">
        <input
          placeholder="Or enter vendor QR code manually"
          value={manualCode}
          onChange={(e) => setManualCode(e.target.value)}
          className="flex-1 rounded-xl border border-sand-300 px-3.5 py-2.5 text-sm focus:border-terracotta-500 focus:outline-none focus:ring-2 focus:ring-terracotta-100"
        />
        <Button type="submit">Go</Button>
      </form>
      <p className="mt-2 text-xs text-sand-400">Try Lucky Taxi's demo code: UKP-VENDOR-LUCKYTAXI-001</p>
    </div>
  );
}
