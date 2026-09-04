import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const homePath = user.userType === "VENDOR" ? "/vendor" : "/dashboard";

  return (
    <nav className="sticky top-0 z-10 flex items-center justify-between bg-white border-b border-slate-200 px-4 py-3 shadow-sm">
      <Link to={homePath} className="flex items-center gap-2">
        <span className="inline-flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600 text-white font-bold">U</span>
        <span className="font-semibold text-slate-800">UKHONA PAY</span>
      </Link>
      <div className="flex items-center gap-3 text-sm">
        <Link to="/platform" title="Platform overview" className="text-lg" aria-label="Platform overview">
          📊
        </Link>
        <span className="text-slate-500 hidden sm:inline">{user.name}</span>
        <button
          onClick={handleLogout}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-slate-600 hover:bg-slate-50"
        >
          Log out
        </button>
      </div>
    </nav>
  );
}
