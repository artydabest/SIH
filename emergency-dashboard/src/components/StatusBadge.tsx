import type { EmergencyStatus } from "../types/emergency";
import { STATUS_LABEL } from "../types/emergency";
import "../styles/status-badge.css";

interface StatusBadgeProps {
  status: EmergencyStatus;
}

/** Semantic status indicator — colored dot + uppercase text, never color alone. */
export default function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`status-badge status-badge--${status.toLowerCase()}`}>
      <span className="status-badge__dot" aria-hidden="true" />
      {STATUS_LABEL[status]}
    </span>
  );
}
