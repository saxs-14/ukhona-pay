import { motion } from "framer-motion";
import { Star } from "lucide-react";
import { spring } from "../lib/motion";

export default function StarRating({ value, onChange, size = 22, readOnly = false }) {
  const stars = [1, 2, 3, 4, 5];

  return (
    <div className="flex gap-1">
      {stars.map((s) => (
        <motion.button
          key={s}
          type="button"
          disabled={readOnly}
          onClick={() => onChange && onChange(s)}
          whileTap={readOnly ? undefined : { scale: 0.8 }}
          whileHover={readOnly ? undefined : { scale: 1.15 }}
          transition={spring}
          className={readOnly ? "cursor-default" : "cursor-pointer"}
          aria-label={`${s} star${s > 1 ? "s" : ""}`}
        >
          <Star
            size={size}
            fill={s <= value ? "#E2971E" : "none"}
            stroke={s <= value ? "#E2971E" : "#C7AE88"}
            strokeWidth={1.75}
          />
        </motion.button>
      ))}
    </div>
  );
}
