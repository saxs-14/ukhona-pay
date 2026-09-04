import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const employeeLinks = [
  { to: "/dashboard", label: "Home", icon: "🏠" },
  { to: "/vendors", label: "Vendors", icon: "🔍" },
  { to: "/scan", label: "Scan", icon: "📷" },
  { to: "/transactions", label: "History", icon: "🧾" },
  { to: "/cashback", label: "Cashback", icon: "💰" },
];

const vendorLinks = [
  { to: "/vendor", label: "Home", icon: "🏠" },
  { to: "/vendor/analytics", label: "Insights", icon: "📊" },
  { to: "/transactions", label: "History", icon: "🧾" },
];

export default function BottomNav() {
  const { user } = useAuth();
  if (!user) return null;
  const links = user.userType === "VENDOR" ? vendorLinks : employeeLinks;

  return (
    <nav className="sticky bottom-0 z-10 flex justify-around border-t border-slate-200 bg-white py-2 shadow-[0_-1px_4px_rgba(0,0,0,0.05)]">
      {links.map((l) => (
        <NavLink
          key={l.to}
          to={l.to}
          className={({ isActive }) =>
            `flex flex-col items-center gap-0.5 px-3 py-1 text-xs ${
              isActive ? "text-blue-600 font-medium" : "text-slate-500"
            }`
          }
        >
          <span className="text-lg leading-none">{l.icon}</span>
          {l.label}
        </NavLink>
      ))}
    </nav>
  );
}
