import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { LogOut } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { dashboardPathFor } from "../lib/roles";
import logo from "../assets/Ukhona Logo.png";

// Deliberately minimal - logo, app name, log out. Every other link (profile,
// platform overview, admin control panel) lives in BottomNav now, alongside
// Home, so there's one place to navigate from instead of two.
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
        <img src={logo} alt="Ukhona Pay" className="h-10 w-10 object-contain" />
        <span className="font-display text-lg text-sand-900">Ukhona Pay</span>
      </Link>
      <motion.button
        whileTap={{ scale: 0.94 }}
        onClick={handleLogout}
        className="flex items-center gap-1.5 rounded-lg border border-sand-300 px-3 py-1.5 text-sm text-sand-600 hover:bg-sand-50"
      >
        <LogOut size={14} />
        Log out
      </motion.button>
    </nav>
  );
}
