export default function Skeleton({ className = "" }) {
  return <div className={`animate-pulse rounded-lg bg-sand-200 ${className}`} />;
}

export function SkeletonCard() {
  return (
    <div className="rounded-2xl border border-sand-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <div className="space-y-2">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-3 w-20" />
        </div>
        <Skeleton className="h-8 w-16" />
      </div>
    </div>
  );
}
