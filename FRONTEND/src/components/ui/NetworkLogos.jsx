// Real South African mobile network brand marks, redrawn as clean inline SVG
// (no external logo files/CDN - keeps the app self-contained and offline-safe).
// Each mirrors the network's actual current public branding: Vodacom's red
// speechmark, MTN's plain yellow lockup (no ellipse - that was retired years
// ago), Telkom's blue "connected dot" T, and Cell C's black-and-white wordmark.

export function VodacomLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="18" cy="18" r="18" fill="#E60000" />
      {/* Speechmark swoosh - a "V"-like curl opening bottom-right, Vodacom's mark since 2017 */}
      <path
        d="M19 9C13.5 9 9 13.5 9 19C9 23.5 12 27.3 16 28.5C15.6 26.9 15.9 25.2 16.9 23.9C15 23.2 13.6 21.3 13.6 19C13.6 16 16 13.6 19 13.6C22 13.6 24.4 16 24.4 19C24.4 21.4 22.9 23.4 20.8 24.1C24.7 23.5 27.7 20.1 27.7 16C27.7 12.1 24.6 9 20.7 9H19Z"
        fill="white"
      />
      <circle cx="19" cy="19" r="3.2" fill="#E60000" />
      <circle cx="19" cy="19" r="3.2" fill="none" stroke="white" strokeWidth="1.1" />
    </svg>
  );
}

export function MtnLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="36" height="36" rx="9" fill="#FFCB05" />
      <text
        x="18"
        y="23.5"
        textAnchor="middle"
        fill="#000000"
        fontFamily="Arial, 'Helvetica Neue', sans-serif"
        fontWeight="800"
        fontSize="13"
        letterSpacing="0.3px"
      >
        MTN
      </text>
    </svg>
  );
}

export function TelkomLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="18" cy="18" r="18" fill="#00A3E0" />
      {/* "Connected dots" T - Telkom's mark of interlinked nodes forming a T */}
      <circle cx="11" cy="12" r="2.6" fill="white" />
      <circle cx="18" cy="12" r="2.6" fill="white" />
      <circle cx="25" cy="12" r="2.6" fill="white" />
      <circle cx="18" cy="18.5" r="2.6" fill="white" />
      <circle cx="18" cy="25" r="2.6" fill="white" />
      <rect x="10.2" y="11.2" width="15.6" height="1.6" rx="0.8" fill="white" />
      <rect x="17.2" y="11.2" width="1.6" height="14.6" rx="0.8" fill="white" />
    </svg>
  );
}

export function CellCLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="18" cy="18" r="18" fill="#000000" />
      <text
        x="18"
        y="23"
        textAnchor="middle"
        fill="#FFFFFF"
        fontFamily="Arial, 'Helvetica Neue', sans-serif"
        fontWeight="800"
        fontSize="12.5"
      >
        C
      </text>
      <circle cx="25" cy="11.5" r="2" fill="#EE1C25" />
    </svg>
  );
}

export default function NetworkLogo({ network, className = "h-7 w-7" }) {
  switch (network?.toLowerCase()) {
    case "vodacom":
      return <VodacomLogo className={className} />;
    case "mtn":
      return <MtnLogo className={className} />;
    case "telkom":
      return <TelkomLogo className={className} />;
    case "cellc":
    case "cell c":
      return <CellCLogo className={className} />;
    default:
      return (
        <div className={`flex items-center justify-center rounded-full bg-sand-200 font-bold text-sand-700 ${className}`}>
          {network?.slice(0, 1)?.toUpperCase() || "?"}
        </div>
      );
  }
}
