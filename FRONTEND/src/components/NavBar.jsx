import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { BarChart3, LogOut, ShieldCheck, UserCog } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { dashboardPathFor } from "../lib/roles";
import logo from "../assets/Ukhona Logo.png";

export default function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const homePath = dashboardPathFor(user.userType);

  return (
    <nav className="sticky top-0 z-20 flex items-center justify-between border-b border-sand-200 bg-white/95 px-4 py-3 backdrop-blur supports-[backdrop-filter]:bg-white/80">
      <Link to={homePath} className="flex items-center gap-2">
        <img src={logo} alt="Ukhona Pay" className="h-8 w-8 object-contain" />
        <span className="font-display text-lg text-sand-900">Ukhona Pay</span>
      </Link>
      <div className="flex items-center gap-3 text-sm">
        {user.userType === "ADMIN" && (
          <Link to="/platform" title="Platform overview" aria-label="Platform overview" className="text-sand-500 hover:text-terracotta-700">
            <BarChart3 size={20} />
          </Link>
        )}
        {user.userType === "ADMIN" && (
          <Link to="/admin" title="Admin control panel" aria-label="Admin control panel" className="text-sand-500 hover:text-terracotta-700">
            <ShieldCheck size={20} />
          </Link>
        )}
        <Link to="/profile" title="Edit profile" aria-label="Edit profile" className="text-sand-500 hover:text-terracotta-700">
          <UserCog size={20} />
        </Link>
        <span className="hidden text-sand-500 sm:inline">{user.name}</span>
        <motion.button
          whileTap={{ scale: 0.94 }}
          onClick={handleLogout}
          className="flex items-center gap-1.5 rounded-lg border border-sand-300 px-3 py-1.5 text-sand-600 hover:bg-sand-50"
        >
          <LogOut size={14} />
          Log out
        </motion.button>
      </div>
    </nav>
  );
}
