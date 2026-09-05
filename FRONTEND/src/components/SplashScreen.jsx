import { motion } from "framer-motion";
import logo from "../assets/Ukhona Logo.png";

// Shown once per app boot (see App.jsx) - a branded 3-second first
// impression while the shell mounts, not a loading gate for real data.
// Still tappable so it never traps a real user - or a developer refreshing
// the page - even at 3s.
export default function SplashScreen({ onSkip }) {
  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0, scale: 1.06 }}
      transition={{ duration: 0.45, ease: [0.4, 0, 1, 1] }}
      onClick={onSkip}
      className="fixed inset-0 z-[100] flex cursor-pointer flex-col items-center justify-center bg-sand-50"
    >
      <motion.div
        animate={{ scale: [1, 1.05, 1] }}
        transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
        className="relative"
      >
        <motion.div
          animate={{ opacity: [0.35, 0.6, 0.35], scale: [1, 1.15, 1] }}
          transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
          className="absolute inset-0 -z-10 rounded-full bg-terracotta-300 blur-2xl"
        />
        <motion.img
          src={logo}
          alt="Ukhona Pay"
          initial={{ scale: 0.5, rotate: -12, opacity: 0 }}
          animate={{ scale: 1, rotate: 0, opacity: 1 }}
          transition={{ type: "spring", stiffness: 220, damping: 15 }}
          className="h-32 w-32 object-contain drop-shadow-lg sm:h-40 sm:w-40"
        />
      </motion.div>

      <motion.h1
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35, duration: 0.4 }}
        className="mt-5 font-display text-2xl text-sand-900"
      >
        Ukhona Pay
      </motion.h1>
      <motion.p
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5, duration: 0.4 }}
        className="mt-1 text-sm text-sand-500"
      >
        Financial identity for taxi-rank traders
      </motion.p>
    </motion.div>
  );
}
