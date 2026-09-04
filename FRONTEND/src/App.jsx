import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import NavBar from "./components/NavBar";
import BottomNav from "./components/BottomNav";
import ProtectedRoute from "./components/ProtectedRoute";
import { useAuth } from "./context/AuthContext";
import { pageVariants } from "./lib/motion";

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
  const location = useLocation();

  return (
    <div className="flex min-h-dvh flex-col bg-sand-50">
      <NavBar />
      <main className="flex-1 pb-4">
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={location.pathname}
            variants={pageVariants}
            initial="initial"
            animate="animate"
            exit="exit"
          >
            <Routes location={location}>
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
          </motion.div>
        </AnimatePresence>
      </main>
      <BottomNav />
    </div>
  );
}
