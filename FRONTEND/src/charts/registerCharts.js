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

export const CHART_COLORS = {
  blue: "#2563eb",
  emerald: "#10b981",
  amber: "#f59e0b",
  slate: "#64748b",
  rose: "#f43f5e",
  violet: "#8b5cf6",
};

export const CATEGORY_COLORS = {
  TAXI: CHART_COLORS.blue,
  FOOD: CHART_COLORS.emerald,
  SERVICES: CHART_COLORS.amber,
  RETAIL: CHART_COLORS.violet,
  OTHER: CHART_COLORS.slate,
};
