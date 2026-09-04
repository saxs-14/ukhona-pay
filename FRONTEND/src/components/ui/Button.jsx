import { motion } from "framer-motion";
import { Loader2 } from "lucide-react";
import { spring, tapScale } from "../../lib/motion";

const variants = {
  primary:
    "bg-terracotta-600 text-white shadow-warm hover:bg-terracotta-700 disabled:bg-terracotta-300",
  secondary:
    "bg-white text-sand-800 border border-sand-300 hover:bg-sand-50 disabled:text-sand-400",
  success:
    "bg-bushveld-600 text-white shadow-warm hover:bg-bushveld-700 disabled:bg-bushveld-300",
  ghost: "text-terracotta-700 hover:bg-terracotta-50 disabled:text-sand-400",
};

export default function Button({
  children,
  variant = "primary",
  loading = false,
  disabled = false,
  className = "",
  type = "button",
  onClick,
  ...rest
}) {
  const isDisabled = disabled || loading;

  return (
    <motion.button
      type={type}
      onClick={onClick}
      disabled={isDisabled}
      whileTap={isDisabled ? undefined : tapScale}
      transition={spring}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-colors duration-150 disabled:cursor-not-allowed ${variants[variant]} ${className}`}
      {...rest}
    >
      {loading && <Loader2 size={16} className="animate-spin" />}
      {children}
    </motion.button>
  );
}
