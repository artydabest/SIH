import { useMemo, useState } from "react";
import type { EmergencyStatus } from "../types/emergency";
import { useEmergencyData } from "../context/EmergencyDataContext";
import StatusBadge from "../components/StatusBadge";
import {
  formatCoords,
  formatTimestampFull,
  MONO,
} from "../lib/format";
import "../styles/page.css";

const FILTERS: Array<{ key: EmergencyStatus | "ALL"; label: string }> = [
  { key: "ALL", label: "All" },
  { key: "NEW", label: "New" },
  { key: "ACKNOWLEDGED", label: "Acknowledged" },
  { key: "RESPONDING", label: "Responding" },
  { key: "RESOLVED", label: "Resolved" },
];

export default function History() {
  const { emergencies, loading } = useEmergencyData();
  const [filter, setFilter] = useState<EmergencyStatus | "ALL">("ALL");

  const sorted = useMemo(() => {
    const list =
      filter === "ALL"
        ? emergencies
        : emergencies.filter((e) => e.status === filter);
    return [...list].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  }, [emergencies, filter]);

  return (
    <div className="page">
      <div className="page__heading">
        <h2>Detection History</h2>
        <p className="page__sub">All detection events, newest first</p>
      </div>

      <div className="filter-row">
        {FILTERS.map(({ key, label }) => (
          <button
            key={key}
            type="button"
            className={`filter-chip ${filter === key ? "filter-chip--active" : ""}`}
            onClick={() => setFilter(key)}
          >
            {label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="muted">Loading history…</p>
      ) : sorted.length === 0 ? (
        <div className="empty-state">
          <h3>NO DETECTIONS</h3>
          <p>No detection events recorded for this filter.</p>
        </div>
      ) : (
        <div className="history-table panel">
          <div className="history-row history-row--head">
            <span>TIME</span>
            <span>DEVICE</span>
            <span>LOCATION</span>
            <span>STATUS</span>
          </div>
          {sorted.map((emergency) => (
            <div key={emergency._id} className="history-row">
              <span className={MONO}>{formatTimestampFull(emergency.createdAt)}</span>
              <span className={MONO}>#{emergency.deviceId}</span>
              <span className={`${MONO} history-row__coords`}>
                {formatCoords(emergency.latitude, emergency.longitude)}
              </span>
              <span>
                <StatusBadge status={emergency.status} />
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
