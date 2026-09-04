import { NavLink, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { BarChart3, History, Home, QrCode, Search, Wallet } from "lucide-react";
import { useAuth } from "../context/AuthContext";

const employeeLinks = [
  { to: "/dashboard", label: "Home", icon: Home },
  { to: "/vendors", label: "Vendors", icon: Search },
  { to: "/scan", label: "Scan", icon: QrCode },
  { to: "/transactions", label: "History", icon: History },
  { to: "/cashback", label: "Cashback", icon: Wallet },
];

const vendorLinks = [
  { to: "/vendor", label: "Home", icon: Home },
  { to: "/vendor/analytics", label: "Insights", icon: BarChart3 },
  { to: "/transactions", label: "History", icon: History },
];

export default function BottomNav() {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return null;
  const links = user.userType === "VENDOR" ? vendorLinks : employeeLinks;

  return (
    <nav className="sticky bottom-0 z-10 flex justify-around border-t border-sand-200 bg-white/95 py-1.5 backdrop-blur supports-[backdrop-filter]:bg-white/80">
      {links.map((l) => {
        const isActive = location.pathname === l.to;
        const Icon = l.icon;
        return (
          <NavLink
            key={l.to}
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
