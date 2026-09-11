import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import type { Emergency } from "../types/emergency";
import { ACTION_LABEL, NEXT_STATUS } from "../types/emergency";
import StatusBadge from "./StatusBadge";
import SeverityChip from "./SeverityChip";
import EvidencePanel from "./EvidencePanel";
import RescueMap from "./RescueMap";
import type { Person } from "../types/person";
import type { SafeZone } from "../types/safezone";
import type { Volunteer } from "../types/volunteer";
import { useNow } from "../hooks/useNow";
import {
  formatAltitude,
  formatConfidence,
  formatCoords,
  formatRelative,
  formatStationary,
  osmUrl,
  MONO,
} from "../lib/format";
import "../styles/incident-detail.css";

interface IncidentDetailProps {
  emergency: Emergency | undefined;
  people: Person[];
  safeZones: SafeZone[];
  volunteers: Volunteer[];
  updatingId: string | null;
  onStatusUpdate: (id: string, nextStatus: Emergency["status"]) => void;
  backTo: string;
}

export default function IncidentDetail({
  emergency,
  people,
  safeZones,
  volunteers,
  updatingId,
  onStatusUpdate,
  backTo,
}: IncidentDetailProps) {
  const navigate = useNavigate();
  const now = useNow(1000);

  if (!emergency) {
    return (
      <div className="empty-state">
        <h3>INCIDENT NOT FOUND</h3>
        <p>This incident may have been removed or the link is stale.</p>
        <Link className="btn btn--primary" to="/incidents">
          ← Back to incidents
        </Link>
      </div>
    );
  }

  const nextStatus = NEXT_STATUS[emergency.status];
  const actionLabel = ACTION_LABEL[emergency.status];
  const isUpdating = updatingId === emergency._id;

  return (
    <div className="incident-detail">
      <button type="button" className="back-link" onClick={() => navigate(backTo)}>
        <ArrowLeft size={14} aria-hidden="true" /> Back
      </button>

      <header className="incident-detail__header">
        <div>
          <p className="incident-detail__eyebrow">POSSIBLE PERSON IN DISTRESS</p>
          <h2 className="mono">Device #{emergency.deviceId}</h2>
        </div>
        <div className="incident-detail__badges">
          <SeverityChip emergency={emergency} />
          <StatusBadge status={emergency.status} />
        </div>
      </header>

      <div className="incident-detail__grid">
        <section className="panel" aria-label="Incident facts">
          <h3 className="panel__title">INCIDENT</h3>
          <dl className="fact-grid">
            <div className="fact">
              <dt>Stationary</dt>
              <dd className={MONO}>{formatStationary(emergency.stationaryMinutes)}</dd>
            </div>
            <div className="fact">
              <dt>Nearby devices</dt>
              <dd className={MONO}>{emergency.nearbyDevices}</dd>
            </div>
            <div className="fact">
              <dt>Status</dt>
              <dd>
                <StatusBadge status={emergency.status} />
              </dd>
            </div>
            <div className="fact">
              <dt>Confidence</dt>
              <dd className={MONO}>
                {formatConfidence(emergency.confidence, emergency.confidenceLevel)}
              </dd>
            </div>
            {emergency.emergencyMode != null && (
              <div className="fact">
                <dt>Emergency mode</dt>
                <dd>{emergency.emergencyMode ? "ACTIVE" : "OFF"}</dd>
              </div>
            )}
            <div className="fact">
              <dt>Detected</dt>
              <dd className={MONO}>{formatRelative(emergency.createdAt, now.getTime())}</dd>
            </div>
            <div className="fact">
              <dt>Coordinates</dt>
              <dd className={MONO}>
                <a
                  href={osmUrl(emergency.latitude, emergency.longitude)}
                  target="_blank"
                  rel="noreferrer"
                >
                  {formatCoords(emergency.latitude, emergency.longitude)}
                </a>
              </dd>
            </div>
            <div className="fact">
              <dt>Altitude</dt>
              <dd className={MONO}>{formatAltitude(emergency.altitude)}</dd>
            </div>
          </dl>

          <EvidencePanel emergency={emergency} />

          <div className="incident-detail__actions">
            {nextStatus && actionLabel ? (
              <button
                type="button"
                className={`btn btn--lg ${
                  emergency.status === "NEW" ? "btn--danger" : "btn--primary"
                }`}
                disabled={isUpdating}
                onClick={() => onStatusUpdate(emergency._id, nextStatus)}
              >
                {isUpdating ? "UPDATING…" : actionLabel}
              </button>
            ) : (
              <p className="resolved-note">Incident resolved — no active action required.</p>
            )}
          </div>
        </section>

        <section className="panel panel--map" aria-label="Incident location">
          <h3 className="panel__title">LAST KNOWN LOCATION</h3>
          <RescueMap
            emergencies={[emergency]}
            people={people}
            safeZones={safeZones}
            volunteers={volunteers}
            selectedId={emergency._id}
            onSelect={() => undefined}
          />
          <p className="map-caption">
            Emergency marker shows relayed position from the detection network.
            Relay devices shown are simulated.
          </p>
        </section>
      </div>
    </div>
  );
}
