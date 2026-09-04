import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Html5Qrcode } from "html5-qrcode";

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
      <h1 className="mb-1 text-lg font-semibold text-slate-800">Scan to pay</h1>
      <p className="mb-4 text-sm text-slate-500">Point your camera at the vendor's UKHONA PAY QR code</p>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-black">
        <div id={SCANNER_ID} className="mx-auto w-full" />
      </div>
      {starting && <p className="mt-2 text-center text-sm text-slate-400">Starting camera...</p>}
      {error && <p className="mt-2 text-sm text-amber-600">{error}</p>}

      <form onSubmit={handleManualSubmit} className="mt-6 flex gap-2">
        <input
          placeholder="Or enter vendor QR code manually"
          value={manualCode}
          onChange={(e) => setManualCode(e.target.value)}
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        <button type="submit" className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
          Go
        </button>
      </form>
      <p className="mt-2 text-xs text-slate-400">Try Lucky Taxi's demo code: UKP-VENDOR-LUCKYTAXI-001</p>
    </div>
  );
}
