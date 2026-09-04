import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import NavBar from "./components/NavBar";
import BottomNav from "./components/BottomNav";
import ProtectedRoute from "./components/ProtectedRoute";
import { useAuth } from "./context/AuthContext";
import { pageVariants } from "./lib/motion";
import { dashboardPathFor } from "./lib/roles";

import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import TransactionHistory from "./pages/TransactionHistory";
import VendorDashboard from "./pages/VendorDashboard";
import DriverDashboard from "./pages/DriverDashboard";
import AssociationAdminDashboard from "./pages/AssociationAdminDashboard";
import VendorAnalytics from "./pages/VendorAnalytics";
import PlatformDashboard from "./pages/PlatformDashboard";

function Home() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={dashboardPathFor(user.userType)} replace />;
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

              <Route path="/vendor" element={<ProtectedRoute allow={["VENDOR"]}><VendorDashboard /></ProtectedRoute>} />
              <Route path="/driver" element={<ProtectedRoute allow={["TAXI_DRIVER"]}><DriverDashboard /></ProtectedRoute>} />
              <Route path="/association-admin" element={<ProtectedRoute allow={["TAXI_ASSOCIATION_ADMIN"]}><AssociationAdminDashboard /></ProtectedRoute>} />
              <Route path="/vendor/analytics" element={<ProtectedRoute allow={["VENDOR", "TAXI_DRIVER"]}><VendorAnalytics /></ProtectedRoute>} />

              <Route path="/transactions" element={<ProtectedRoute allow={["VENDOR", "TAXI_DRIVER"]}><TransactionHistory /></ProtectedRoute>} />
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
