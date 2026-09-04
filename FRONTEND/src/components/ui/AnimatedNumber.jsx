import { useEffect, useRef, useState } from "react";
import { animate, useReducedMotion } from "framer-motion";

export default function AnimatedNumber({ value, prefix = "", decimals = 2, className = "" }) {
  const shouldReduceMotion = useReducedMotion();
  const [display, setDisplay] = useState(value);
  const prevValue = useRef(value);

  useEffect(() => {
    if (shouldReduceMotion) {
      setDisplay(value);
      prevValue.current = value;
      return;
    }
    const controls = animate(prevValue.current, value, {
      duration: 0.6,
      ease: [0.16, 1, 0.3, 1],
      onUpdate: (v) => setDisplay(v),
    });
    prevValue.current = value;
    return controls.stop;
  }, [value, shouldReduceMotion]);

  return (
    <span className={className}>
      {prefix}
      {display.toFixed(decimals)}
    </span>
  );
}
