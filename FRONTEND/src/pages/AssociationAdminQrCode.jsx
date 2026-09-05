import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { ArrowLeft, Building2, Copy, QrCode, ShieldCheck } from "lucide-react";
import client from "../api/client";
import { SkeletonCard } from "../components/ui/Skeleton";
import { ease } from "../lib/motion";

export default function AssociationAdminQrCode() {
  const [profile, setProfile] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    client.get("/users/me").then((res) => setProfile(res.data));
  }, []);

  if (!profile) {
    return (
      <div className="mx-auto max-w-md space-y-3 px-4 py-6">
        <SkeletonCard />
      </div>
    );
  }

  const code = `UKP-ASSOC-${profile.associationId}`;

  function handleCopy() {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <div className="mx-auto max-w-md px-4 py-6">
      <Link to="/association-admin" className="mb-3 inline-flex items-center gap-1 text-sm font-medium text-sand-500 hover:text-terracotta-700">
        <ArrowLeft size={15} /> Back
      </Link>
      <h1 className="mb-1 flex items-center gap-2 font-display text-xl text-sand-900">
        <QrCode size={20} className="text-terracotta-600" /> Association QR Code
      </h1>
      <p className="mb-4 text-sm text-sand-500">{profile.associationName || "Your association"}</p>

      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.35, ease: ease.enter }}
        className="rounded-2xl border border-sand-200 bg-white p-6 text-center shadow-warm"
      >
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-terracotta-600 shadow-warm">
          <Building2 size={22} className="text-white" />
        </div>
        <p className="mb-4 flex items-center justify-center gap-1.5 text-sm font-semibold text-sand-800">
          {profile.associationName}
          <ShieldCheck size={14} className="text-terracotta-600" />
        </p>

        <div className="mx-auto flex w-fit items-center justify-center rounded-xl bg-sand-50 p-4">
          <QRCodeSVG value={code} size={200} level="M" />
        </div>

        <button
          onClick={handleCopy}
          className="mx-auto mt-4 flex items-center gap-1.5 rounded-lg border border-sand-200 px-3 py-1.5 text-xs font-medium text-sand-600 transition-colors hover:bg-sand-50"
        >
          <Copy size={13} /> {copied ? "Copied!" : code}
        </button>
      </motion.div>

      <div className="mt-5 rounded-xl bg-sand-50 p-4 text-sm text-sand-600">
        Show this code to a driver during in-person verification, or when they need to confirm
        which taxi association they're registering under. It identifies{" "}
        <span className="font-medium text-sand-800">{profile.associationName}</span> only — it
        isn't used to move money.
      </div>
    </div>
  );
}
