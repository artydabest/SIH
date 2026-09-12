import { useState } from "react";
import { useEmergencyData } from "../context/EmergencyDataContext";
import RescueMap from "../components/RescueMap";
import IncidentCard from "../components/IncidentCard";
import ActiveWarningsPanel from "../components/ActiveWarningsPanel";
import { useNow } from "../hooks/useNow";
import "../styles/page.css";

export default function MapPage() {
  const { emergencies, people, safeZones, volunteers, activeAlert, warnings, loading, updatingId, updateStatus } =
    useEmergencyData();
  const now = useNow(1000);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const active = emergencies.filter((e) => e.status !== "RESOLVED");

  return (
    <div className="page">
      <div className="page__heading">
        <h2>Rescue Map</h2>
        <p className="page__sub">
          Emergency locations with nearby relay devices and responder position
        </p>
      </div>

      <div className="dashboard-warnings">
        <ActiveWarningsPanel warnings={warnings} />
      </div>

      <div className="map-page-grid">
        <section className="panel panel--map panel--tall" aria-label="Rescue map">
          <RescueMap
            emergencies={emergencies}
            people={people}
            safeZones={safeZones}
            volunteers={volunteers}
            activeAlert={activeAlert}
            selectedId={selectedId}
            onSelect={(emergency) => setSelectedId(emergency?._id ?? null)}
          />
        </section>

        <aside className="map-page-side">
          <h3 className="panel__title">EMERGENCIES</h3>
          {loading ? (
            <p className="muted">Loading incidents…</p>
          ) : active.length === 0 ? (
            <p className="muted">No active incidents to display.</p>
          ) : (
            active.map((emergency) => (
              <IncidentCard
                key={emergency._id}
                emergency={emergency}
                now={now.getTime()}
                updatingId={updatingId}
                onStatusUpdate={(id, nextStatus) => void updateStatus(id, nextStatus)}
                onSelect={(emergency) => setSelectedId(emergency._id)}
                selected={selectedId === emergency._id}
              />
            ))
          )}
        </aside>
      </div>
    </div>
  );
}
