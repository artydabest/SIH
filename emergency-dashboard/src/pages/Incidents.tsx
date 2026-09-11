import { useMemo, useState } from "react";
import type { EmergencyStatus } from "../types/emergency";
import { useEmergencyData } from "../context/EmergencyDataContext";
import { useNow } from "../hooks/useNow";
import IncidentList from "../components/IncidentList";
import RescueMap from "../components/RescueMap";
import "../styles/page.css";

const FILTERS: Array<{ key: EmergencyStatus | "ALL"; label: string }> = [
  { key: "ALL", label: "All" },
  { key: "NEW", label: "New" },
  { key: "ACKNOWLEDGED", label: "Acknowledged" },
  { key: "RESPONDING", label: "Responding" },
  { key: "RESOLVED", label: "Resolved" },
];

export default function Incidents() {
  const { emergencies, people, safeZones, volunteers, loading, updatingId, updateStatus } =
    useEmergencyData();
  const now = useNow(1000);
  const [filter, setFilter] = useState<EmergencyStatus | "ALL">("ALL");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const filtered = useMemo(
    () =>
      filter === "ALL"
        ? emergencies
        : emergencies.filter((e) => e.status === filter),
    [emergencies, filter]
  );

  return (
    <div className="page">
      <div className="page__heading">
        <h2>Active Incidents</h2>
        <p className="page__sub">
          Select an incident to focus the map and review evidence
        </p>
      </div>

      <div className="filter-row" role="tablist" aria-label="Filter incidents by status">
        {FILTERS.map(({ key, label }) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={filter === key}
            className={`filter-chip ${filter === key ? "filter-chip--active" : ""}`}
            onClick={() => setFilter(key)}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="incidents-grid">
        <IncidentList
          emergencies={filtered}
          loading={loading}
          now={now.getTime()}
          updatingId={updatingId}
          selectedId={selectedId}
          onStatusUpdate={(id, nextStatus) => void updateStatus(id, nextStatus)}
          onSelect={(emergency) => setSelectedId(emergency._id)}
        />
        <section className="panel panel--map" aria-label="Incident map">
          <div className="panel__heading">
            <h3 className="panel__title">RESCUE MAP</h3>
          </div>
          <RescueMap
            emergencies={filtered}
            people={people}
            safeZones={safeZones}
            volunteers={volunteers}
            selectedId={selectedId}
            onSelect={(emergency) => setSelectedId(emergency?._id ?? null)}
          />
        </section>
      </div>
    </div>
  );
}
