import { useNavigate } from "react-router-dom";
import { useEmergencyData } from "../context/EmergencyDataContext";
import { useNow } from "../hooks/useNow";
import SummaryCards from "../components/SummaryCards";
import IncidentList from "../components/IncidentList";
import RescueMap from "../components/RescueMap";
import SafeZoneCard from "../components/SafeZoneCard";
import "../styles/page.css";

export default function Dashboard() {
  const { emergencies, people, safeZones, volunteers, activeAlert, loading, updatingId, updateStatus } =
    useEmergencyData();
  const now = useNow(1000);
  const navigate = useNavigate();

  const active = emergencies.filter((e) => e.status !== "RESOLVED");

  const handleSelect = (emergency: { _id: string } | null) => {
    if (emergency) {
      navigate(`/incidents/${emergency._id}`);
    }
  };

  return (
    <div className="page">
      <div className="page__heading">
        <h2>Dashboard</h2>
        <p className="page__sub">Active incidents across the detection network</p>
      </div>

      <SummaryCards emergencies={emergencies} loading={loading} />

      <div className="dashboard-grid">
        <section className="panel" aria-label="Active incidents">
          <div className="panel__heading">
            <h3 className="panel__title">ACTIVE INCIDENTS</h3>
            <span className="panel__count mono">{active.length}</span>
          </div>
          <IncidentList
            emergencies={active}
            loading={loading}
            now={now.getTime()}
            updatingId={updatingId}
            selectedId={null}
            onStatusUpdate={(id, nextStatus) => void updateStatus(id, nextStatus)}
            onSelect={handleSelect}
          />
        </section>

        <div className="dashboard-grid__side">
          <section className="panel panel--map" aria-label="Live rescue map">
            <div className="panel__heading">
              <h3 className="panel__title">LIVE RESCUE MAP</h3>
              <span className="live-chip">● LIVE</span>
            </div>
            <RescueMap
              emergencies={emergencies}
              people={people}
              safeZones={safeZones}
              volunteers={volunteers}
              activeAlert={activeAlert}
              selectedId={null}
              onSelect={handleSelect}
            />
          </section>
          <SafeZoneCard safeZones={safeZones} />
        </div>
      </div>
    </div>
  );
}
