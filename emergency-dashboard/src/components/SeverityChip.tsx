import type { Emergency } from "../types/emergency";
import { severityOf } from "../lib/format";
import "../styles/severity-chip.css";

interface SeverityChipProps {
  emergency: Emergency;
}

/**
 * Severity is DERIVED from workflow status + evidence (stationary duration),
 * never fabricated as a backend field.
 */
export default function SeverityChip({ emergency }: SeverityChipProps) {
  const severity = severityOf(emergency.status);
  return (
    <span className={`severity-chip severity-chip--${severity.toLowerCase()}`}>
      {severity} priority
    </span>
  );
}
