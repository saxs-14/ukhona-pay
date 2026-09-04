import { motion } from "framer-motion";
import { listItem } from "../../lib/motion";

export default function Card({ children, className = "", as: Component = motion.div, interactive = false, ...rest }) {
  return (
    <Component
      variants={listItem}
      whileHover={interactive ? { y: -2, boxShadow: "0 12px 32px -8px rgba(91, 43, 22, 0.22)" } : undefined}
      whileTap={interactive ? { scale: 0.98 } : undefined}
      className={`rounded-2xl border border-sand-200 bg-white p-4 shadow-sm ${interactive ? "cursor-pointer" : ""} ${className}`}
      {...rest}
    >
      {children}
    </Component>
  );
}
