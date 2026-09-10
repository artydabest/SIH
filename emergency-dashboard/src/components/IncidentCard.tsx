import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import type { Emergency } from "../types/emergency";
import { ACTION_LABEL, NEXT_STATUS } from "../types/emergency";
import StatusBadge from "./StatusBadge";
import SeverityChip from "./SeverityChip";
import {
  formatRelative,
  formatStationary,
  MONO,
} from "../lib/format";
import "../styles/incident-card.css";

interface IncidentCardProps {
  emergency: Emergency;
  now: number;
  updatingId: string | null;
  onStatusUpdate: (id: string, nextStatus: Emergency["status"]) => void;
  onSelect: (emergency: Emergency) => void;
  selected: boolean;
}

export default function IncidentCard({
  emergency,
  now,
  updatingId,
  onStatusUpdate,
  onSelect,
  selected,
}: IncidentCardProps) {
  const nextStatus = NEXT_STATUS[emergency.status];
  const actionLabel = ACTION_LABEL[emergency.status];
  const isUpdating = updatingId === emergency._id;
  const resolved = emergency.status === "RESOLVED";

  return (
    <article
      className={`incident-card ${resolved ? "incident-card--resolved" : ""} ${
        selected ? "incident-card--selected" : ""
      }`}
      aria-label={`Incident from device ${emergency.deviceId}`}
    >
      <header className="incident-card__top">
        <div className="incident-card__id">
          <span className="mono">Device #{emergency.deviceId}</span>
          <SeverityChip emergency={emergency} />
        </div>
        <StatusBadge status={emergency.status} />
      </header>

      <button
        type="button"
        className="incident-card__body"
        onClick={() => onSelect(emergency)}
        aria-label={`Open incident ${emergency.deviceId} on map`}
      >
        <div className="incident-card__metrics">
          <div className="metric">
            <span className="metric__label">Stationary</span>
            <span className={`metric__value ${MONO}`}>
              {formatStationary(emergency.stationaryMinutes)}
            </span>
          </div>
          <div className="metric">
            <span className="metric__label">Nearby devices</span>
            <span className={`metric__value ${MONO}`}>{emergency.nearbyDevices}</span>
          </div>
          <div className="metric">
            <span className="metric__label">Detected</span>
            <span className={`metric__value ${MONO}`}>
              {formatRelative(emergency.createdAt, now)}
            </span>
          </div>
        </div>
      </button>

      <footer className="incident-card__actions">
        {nextStatus && actionLabel ? (
          <button
            type="button"
            className={`btn ${
              emergency.status === "NEW" ? "btn--danger" : "btn--primary"
            }`}
            disabled={isUpdating}
            onClick={() => onStatusUpdate(emergency._id, nextStatus)}
          >
            {isUpdating ? "UPDATING…" : actionLabel}
          </button>
        ) : (
          <span className="incident-card__done">Incident resolved — no action required</span>
        )}
        <Link
          className="incident-card__view"
          to={`/incidents/${emergency._id}`}
          aria-label={`View details for device ${emergency.deviceId}`}
        >
          VIEW INCIDENT <ArrowRight size={13} aria-hidden="true" />
        </Link>
      </footer>
    </article>
  );
}
