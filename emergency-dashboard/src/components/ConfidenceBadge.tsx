import type { Emergency } from "../types/emergency";
import { confidenceMeta } from "../types/confidence";
import "../styles/confidence-badge.css";

interface ConfidenceBadgeProps {
  emergency: Emergency;
}

/**
 * Evidence-strength indicator. Colors follow the product spec:
 * LOW = green, MEDIUM = yellow, HIGH = orange, CRITICAL = red.
 * Never confuse this with the responder status badge.
 */
export default function ConfidenceBadge({ emergency }: ConfidenceBadgeProps) {
  const meta = confidenceMeta(emergency.confidenceLevel);

  // Old records predate confidence scoring — show a neutral placeholder.
  if (!meta || emergency.confidence == null) {
    return <span className="confidence-badge confidence-badge--unknown">—</span>;
  }

  return (
    <span className={`confidence-badge confidence-badge--${meta.className}`}>
      <span className="confidence-badge__dot" aria-hidden="true" />
      {emergency.confidence}% · {meta.label}
    </span>
  );
}
