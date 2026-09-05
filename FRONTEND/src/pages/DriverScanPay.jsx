import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import client from "../api/client";
import ScanAndPayModal from "../components/ScanAndPayModal";

export default function DriverScanPay() {
  const navigate = useNavigate();
  const [wallet, setWallet] = useState(null);

  useEffect(() => {
    client.get("/wallet/me").then((res) => setWallet(res.data)).catch(() => {});
  }, []);

  return (
    <ScanAndPayModal
      isOpen={true}
      onClose={() => navigate("/driver")}
      walletBalance={wallet?.balance || 0}
      onSuccess={() => navigate("/driver")}
    />
  );
}

