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
import DriverWithdraw from "./pages/DriverWithdraw";
import DriverSendMoney from "./pages/DriverSendMoney";
import DriverScanPay from "./pages/DriverScanPay";
import BuyServices from "./pages/BuyServices";
import AssociationAdminDashboard from "./pages/AssociationAdminDashboard";
import AssociationAdminDrivers from "./pages/AssociationAdminDrivers";
import AssociationAdminQrCode from "./pages/AssociationAdminQrCode";
import AdminDashboard from "./pages/AdminDashboard";
import VendorAnalytics from "./pages/VendorAnalytics";
import DriverAnalytics from "./pages/DriverAnalytics";
import PlatformDashboard from "./pages/PlatformDashboard";
import PayVendor from "./pages/PayVendor";
import EditProfile from "./pages/EditProfile";

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
              <Route path="/pay/:qrCode" element={<PayVendor />} />

              <Route path="/vendor" element={<ProtectedRoute allow={["VENDOR"]}><VendorDashboard /></ProtectedRoute>} />
              <Route path="/vendor/buy" element={<ProtectedRoute allow={["VENDOR"]}><BuyServices /></ProtectedRoute>} />
              <Route path="/driver" element={<ProtectedRoute allow={["TAXI_DRIVER"]}><DriverDashboard /></ProtectedRoute>} />
              <Route path="/driver/withdraw" element={<ProtectedRoute allow={["TAXI_DRIVER"]}><DriverWithdraw /></ProtectedRoute>} />
              <Route path="/driver/send" element={<ProtectedRoute allow={["TAXI_DRIVER"]}><DriverSendMoney /></ProtectedRoute>} />
              <Route path="/driver/buy" element={<ProtectedRoute allow={["TAXI_DRIVER"]}><BuyServices /></ProtectedRoute>} />
              <Route path="/driver/scan" element={<ProtectedRoute allow={["TAXI_DRIVER"]}><DriverScanPay /></ProtectedRoute>} />
              <Route path="/driver/analytics" element={<ProtectedRoute allow={["TAXI_DRIVER"]}><DriverAnalytics /></ProtectedRoute>} />
              <Route path="/association-admin" element={<ProtectedRoute allow={["TAXI_ASSOCIATION_ADMIN"]}><AssociationAdminDashboard /></ProtectedRoute>} />
              <Route path="/association-admin/drivers" element={<ProtectedRoute allow={["TAXI_ASSOCIATION_ADMIN"]}><AssociationAdminDrivers /></ProtectedRoute>} />
              <Route path="/association-admin/qr" element={<ProtectedRoute allow={["TAXI_ASSOCIATION_ADMIN"]}><AssociationAdminQrCode /></ProtectedRoute>} />
              <Route path="/admin" element={<ProtectedRoute allow={["ADMIN"]}><AdminDashboard /></ProtectedRoute>} />
              <Route path="/vendor/analytics" element={<ProtectedRoute allow={["VENDOR"]}><VendorAnalytics /></ProtectedRoute>} />

              <Route path="/transactions" element={<ProtectedRoute allow={["VENDOR", "TAXI_DRIVER"]}><TransactionHistory /></ProtectedRoute>} />
              <Route path="/profile" element={<ProtectedRoute><EditProfile /></ProtectedRoute>} />
              <Route path="/platform" element={<ProtectedRoute allow={["ADMIN"]}><PlatformDashboard /></ProtectedRoute>} />

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </motion.div>
        </AnimatePresence>
      </main>
      <BottomNav />
    </div>
  );
}
