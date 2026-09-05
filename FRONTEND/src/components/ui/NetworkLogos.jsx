export function VodacomLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="18" cy="18" r="18" fill="#E60000" />
      {/* Vodacom Speechmark */}
      <path
        d="M18 8C12.48 8 8 12.48 8 18C8 22.95 11.6 27.05 16.3 27.85V23.3C13.8 22.4 12 20.4 12 18C12 14.69 14.69 12 18 12C21.31 12 24 14.69 24 18C24 19.78 23.22 21.38 21.97 22.48L24.8 25.31C26.78 23.51 28 20.9 28 18C28 12.48 23.52 8 18 8Z"
        fill="white"
      />
      <circle cx="18" cy="18" r="2.8" fill="#E60000" />
    </svg>
  );
}

export function MtnLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="18" cy="18" r="18" fill="#FFCC00" />
      <ellipse cx="18" cy="18" rx="13.2" ry="8.2" stroke="#000000" strokeWidth="2.2" fill="none" />
      <text
        x="18"
        y="21.2"
        textAnchor="middle"
        fill="#000000"
        fontFamily="system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
        fontWeight="900"
        fontSize="8.5"
        letterSpacing="-0.2px"
        fontStyle="italic"
      >
        MTN
      </text>
    </svg>
  );
}

export function TelkomLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="18" cy="18" r="18" fill="#005C9E" />
      {/* Telkom Connected Nodes "T" Symbol */}
      <rect x="11.5" y="11.5" width="13" height="2.5" rx="1" fill="white" />
      <rect x="16.75" y="12.5" width="2.5" height="12.5" rx="1" fill="white" />
      <circle cx="11.5" cy="12.75" r="2.8" fill="white" />
      <circle cx="18" cy="12.75" r="2.8" fill="white" />
      <circle cx="24.5" cy="12.75" r="2.8" fill="white" />
      <circle cx="18" cy="18.75" r="2.8" fill="white" />
      <circle cx="18" cy="24.75" r="2.8" fill="white" />
    </svg>
  );
}

export function CellCLogo({ className = "h-7 w-7" }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="18" cy="18" r="18" fill="#121212" />
      {/* White 'C' with orange apostrophe */}
      <path
        d="M21.2 13.8C20.2 12.5 18.6 11.8 16.8 11.8C13.3 11.8 10.6 14.5 10.6 18C10.6 21.5 13.3 24.2 16.8 24.2C18.6 24.2 20.2 23.5 21.2 22.2"
        stroke="white"
        strokeWidth="3.2"
        strokeLinecap="round"
      />
      {/* Cell C signature orange apostrophe dot */}
      <circle cx="23.2" cy="12.2" r="1.7" fill="#FF5500" />
      <path d="M23.2 13.2L22.2 16H24.2L23.2 13.2Z" fill="#FF5500" />
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
