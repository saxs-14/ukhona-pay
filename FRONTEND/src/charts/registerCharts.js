import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip,
} from "chart.js";

// Registering is idempotent, so every page that needs charts can import this safely.
ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, LineElement, PointElement, Tooltip, Legend);

// Matches the tailwind.config.js terracotta/bushveld/gold/sand palette so
// charts read as part of the same product, not a bolted-on default theme.
export const CHART_COLORS = {
  terracotta: "#C96328",
  bushveld: "#2F8F4E",
  gold: "#E2971E",
  violet: "#8B5CF6",
  slate: "#8A6E4C",
};

export const CATEGORY_COLORS = {
  TAXI: CHART_COLORS.terracotta,
  FOOD: CHART_COLORS.bushveld,
  SERVICES: CHART_COLORS.gold,
  RETAIL: CHART_COLORS.violet,
  OTHER: CHART_COLORS.slate,
};
