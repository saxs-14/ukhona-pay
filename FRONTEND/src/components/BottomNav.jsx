import { NavLink, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
<<<<<<< HEAD
import { BarChart3, History, Home, ScanLine } from "lucide-react";
=======
import { BarChart3, History, Home, QrCode, ScanLine, Users } from "lucide-react";
>>>>>>> origin/main
import { useAuth } from "../context/AuthContext";

const traderLinks = (home) => [
  { to: home, label: "Home", icon: Home },
  { to: "/vendor/analytics", label: "Insights", icon: BarChart3 },
  { to: "/transactions", label: "History", icon: History },
];

const driverLinks = [
  { to: "/driver", label: "Home", icon: Home },
  { to: "/driver/scan", label: "Pay", icon: ScanLine },
  { to: "/driver/analytics", label: "Insights", icon: BarChart3 },
  { to: "/transactions", label: "History", icon: History },
];

const adminLinks = [
  { to: "/association-admin", label: "Home", icon: Home },
<<<<<<< HEAD
=======
  { to: "/association-admin/drivers", label: "Drivers", icon: Users },
  { to: "/association-admin/qr", label: "QR Code", icon: QrCode },
];

const platformAdminLinks = [
  { to: "/admin", label: "Home", icon: Home },
>>>>>>> origin/main
];

export default function BottomNav() {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return null;

  let links;
  if (user.userType === "VENDOR") links = traderLinks("/vendor");
  else if (user.userType === "TAXI_DRIVER") links = driverLinks;
<<<<<<< HEAD
=======
  else if (user.userType === "ADMIN") links = platformAdminLinks;
>>>>>>> origin/main
  else links = adminLinks;

  return (
    <nav className="sticky bottom-0 z-10 flex justify-around border-t border-sand-200 bg-white/95 py-1.5 backdrop-blur supports-[backdrop-filter]:bg-white/80">
      {links.map((l) => {
        const isActive = location.pathname === l.to;
        const Icon = l.icon;
        return (
          <NavLink
            key={l.label}
            to={l.to}
            className="relative flex flex-col items-center gap-0.5 px-4 py-1.5 text-xs"
          >
            {isActive && (
              <motion.div
                layoutId="bottom-nav-indicator"
                className="absolute inset-x-1 top-0 h-0.5 rounded-full bg-terracotta-600"
                transition={{ type: "spring", stiffness: 380, damping: 30 }}
              />
            )}
            <Icon
              size={20}
              strokeWidth={2}
              className={isActive ? "text-terracotta-700" : "text-sand-500"}
            />
            <span className={isActive ? "font-semibold text-terracotta-700" : "text-sand-500"}>
              {l.label}
            </span>
          </NavLink>
        );
      })}
    </nav>
  );
}
