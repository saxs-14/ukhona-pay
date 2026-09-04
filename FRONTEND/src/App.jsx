import { Navigate, Route, Routes } from "react-router-dom";
import NavBar from "./components/NavBar";
import BottomNav from "./components/BottomNav";
import ProtectedRoute from "./components/ProtectedRoute";
import { useAuth } from "./context/AuthContext";

import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import EmployeeDashboard from "./pages/EmployeeDashboard";
import VendorSearch from "./pages/VendorSearch";
import VendorDetail from "./pages/VendorDetail";
import ScanQr from "./pages/ScanQr";
import TransactionHistory from "./pages/TransactionHistory";
import CashbackWallet from "./pages/CashbackWallet";
import VendorDashboard from "./pages/VendorDashboard";
import VendorAnalytics from "./pages/VendorAnalytics";
import PlatformDashboard from "./pages/PlatformDashboard";

function Home() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.userType === "VENDOR" ? "/vendor" : "/dashboard"} replace />;
}

export default function App() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <NavBar />
      <main className="flex-1 pb-4">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route path="/dashboard" element={<ProtectedRoute allow={["EMPLOYEE", "CORPORATE"]}><EmployeeDashboard /></ProtectedRoute>} />
          <Route path="/vendors" element={<ProtectedRoute allow={["EMPLOYEE", "CORPORATE"]}><VendorSearch /></ProtectedRoute>} />
          <Route path="/vendors/:qrCode" element={<ProtectedRoute allow={["EMPLOYEE", "CORPORATE"]}><VendorDetail /></ProtectedRoute>} />
          <Route path="/scan" element={<ProtectedRoute allow={["EMPLOYEE", "CORPORATE"]}><ScanQr /></ProtectedRoute>} />
          <Route path="/cashback" element={<ProtectedRoute allow={["EMPLOYEE", "CORPORATE"]}><CashbackWallet /></ProtectedRoute>} />

          <Route path="/vendor" element={<ProtectedRoute allow={["VENDOR"]}><VendorDashboard /></ProtectedRoute>} />
          <Route path="/vendor/analytics" element={<ProtectedRoute allow={["VENDOR"]}><VendorAnalytics /></ProtectedRoute>} />

          <Route path="/transactions" element={<ProtectedRoute><TransactionHistory /></ProtectedRoute>} />
          <Route path="/platform" element={<ProtectedRoute><PlatformDashboard /></ProtectedRoute>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <BottomNav />
    </div>
  );
}
