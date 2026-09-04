// Shared motion tokens so every animation in the app shares the same rhythm
// instead of each component inventing its own timing.

export const spring = { type: "spring", mass: 1, damping: 18, stiffness: 220 };
export const springSoft = { type: "spring", mass: 1, damping: 22, stiffness: 160 };

export const ease = {
  enter: [0.16, 1, 0.3, 1], // ease-out - decelerate into place
  exit: [0.4, 0, 1, 1], // ease-in - accelerate away
};

export const durations = {
  micro: 0.15,
  short: 0.22,
  medium: 0.32,
};

// Page-level transition: forward navigation slides up + fades in, exits are
// faster than entrances (~65%) per standard motion-design guidance.
export const pageVariants = {
  initial: { opacity: 0, y: 12 },
  animate: { opacity: 1, y: 0, transition: { duration: durations.medium, ease: ease.enter } },
  exit: { opacity: 0, y: -8, transition: { duration: durations.medium * 0.65, ease: ease.exit } },
};

// List container + item variants for staggered entrances (30-50ms per item).
export const listContainer = {
  animate: { transition: { staggerChildren: 0.05, delayChildren: 0.04 } },
};

export const listItem = {
  initial: { opacity: 0, y: 10 },
  animate: { opacity: 1, y: 0, transition: { duration: durations.short, ease: ease.enter } },
};

export const tapScale = { scale: 0.96 };
export const hoverLift = { y: -2 };
