import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Award, ShieldCheck, TrendingUp } from "lucide-react";
import client from "../../api/client";
import AnimatedNumber from "./AnimatedNumber";
import { SkeletonCard } from "./Skeleton";

const RADIUS = 52;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

function ScoreRing({ score, eligible }) {
  const [progress, setProgress] = useState(0);
  useEffect(() => {
    const t = setTimeout(() => setProgress(score), 150);
    return () => clearTimeout(t);
  }, [score]);

  const color = eligible ? "#1C5B35" : "#C17A12";
  const offset = CIRCUMFERENCE - (progress / 100) * CIRCUMFERENCE;

  return (
    <div className="relative flex h-36 w-36 items-center justify-center">
      <svg width="144" height="144" viewBox="0 0 144 144" className="-rotate-90">
        <circle cx="72" cy="72" r={RADIUS} fill="none" stroke="#F3E9D6" strokeWidth="12" />
        <motion.circle
          cx="72"
          cy="72"
          r={RADIUS}
          fill="none"
          stroke={color}
          strokeWidth="12"
          strokeLinecap="round"
          strokeDasharray={CIRCUMFERENCE}
          initial={{ strokeDashoffset: CIRCUMFERENCE }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 1, ease: [0.16, 1, 0.3, 1] }}
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="font-display text-3xl text-sand-900">
          <AnimatedNumber value={score} decimals={0} />
        </span>
        <span className="text-xs text-sand-500">/ 100</span>
      </div>
    </div>
  );
}

export default function FinancialScoreCard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    client
      .get("/analytics/financial-score/me")
      .then((res) => setData(res.data))
      .catch(() => setError(true));
  }, []);

  if (error) return null;
  if (!data) return <SkeletonCard />;

  const progressToTarget = Math.min(100, Math.round((data.daysRecorded / data.windowDays) * 100));

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.15 }}
      className="mt-5 rounded-2xl border border-sand-200 bg-white p-5 shadow-sm"
    >
      <div className="mb-4 flex items-center justify-between">
        <h2 className="flex items-center gap-1.5 text-sm font-semibold text-sand-700">
          <Award size={15} className="text-gold-600" /> Financial Identity
        </h2>
        <span
          className={`flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ${
            data.creditEligible ? "bg-bushveld-50 text-bushveld-700" : "bg-gold-500/10 text-gold-600"
          }`}
        >
          <ShieldCheck size={12} />
          {data.creditEligible ? "Credit eligible" : "Building identity"}
        </span>
      </div>

      <div className="flex items-center gap-5">
        <ScoreRing score={data.financialScore} eligible={data.creditEligible} />
        <div className="flex-1 space-y-2.5">
          <div>
            <div className="mb-1 flex justify-between text-xs text-sand-500">
              <span>Days recorded</span>
              <span>{data.daysRecorded} / {data.windowDays}</span>
            </div>
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-sand-100">
              <motion.div
                className="h-full rounded-full bg-terracotta-500"
                initial={{ width: 0 }}
                animate={{ width: `${progressToTarget}%` }}
                transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
              />
            </div>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-sand-500">Consistency</span>
            <span className="font-medium text-sand-800">{data.consistencyPercentage}%</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-sand-500">Verified income</span>
            <span className="font-medium text-sand-800">
              <AnimatedNumber value={Number(data.totalIncome)} prefix="R" />
            </span>
          </div>
        </div>
      </div>

      {data.creditEligible ? (
        <div className="mt-4 flex items-start gap-2 rounded-xl bg-bushveld-50 p-3 text-sm text-bushveld-800">
          <TrendingUp size={16} className="mt-0.5 shrink-0" />
          <p>
            Potential lending range{" "}
            <strong>
              R{Number(data.recommendedMin).toLocaleString("en-ZA")} – R{Number(data.recommendedMax).toLocaleString("en-ZA")}
            </strong>
            , based on {data.reason}. Subject to lender assessment — this is a readiness indicator, not a guaranteed loan.
          </p>
        </div>
      ) : (
        <p className="mt-4 rounded-xl bg-sand-50 p-3 text-sm text-sand-600">{data.reason}.</p>
      )}
    </motion.div>
  );
}
