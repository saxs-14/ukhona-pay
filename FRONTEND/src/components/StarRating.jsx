export default function StarRating({ value, onChange, size = "text-2xl", readOnly = false }) {
  const stars = [1, 2, 3, 4, 5];

  return (
    <div className={`flex gap-1 ${size}`}>
      {stars.map((s) => (
        <button
          key={s}
          type="button"
          disabled={readOnly}
          onClick={() => onChange && onChange(s)}
          className={`leading-none ${readOnly ? "cursor-default" : "cursor-pointer"} ${
            s <= value ? "text-amber-400" : "text-slate-300"
          }`}
          aria-label={`${s} star${s > 1 ? "s" : ""}`}
        >
          ★
        </button>
      ))}
    </div>
  );
}
