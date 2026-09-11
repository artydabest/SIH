import { useNavigate } from "react-router-dom";
import { useEmergencyData } from "../context/EmergencyDataContext";
import { useNow } from "../hooks/useNow";
import SummaryCards from "../components/SummaryCards";
import IncidentList from "../components/IncidentList";
import RescueMap from "../components/RescueMap";
import SafeZoneCard from "../components/SafeZoneCard";
import "../styles/page.css";

export default function Dashboard() {
  const { emergencies, loading, updatingId, updateStatus, dataSource } =
    useEmergencyData();
  const now = useNow(1000);
  const navigate = useNavigate();

  const active = emergencies.filter((e) => e.status !== "RESOLVED");

  const handleSelect = (emergency: { _id: string }) => {
    navigate(`/incidents/${emergency._id}`);
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
            <h3 className="panel__title">Active incidents</h3>
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
            demoMode={dataSource === "demo"}
          />
        </section>

        <div className="dashboard-grid__side">
          <section className="panel panel--map" aria-label="Live rescue map">
            <div className="panel__heading">
              <h3 className="panel__title">Rescue map</h3>
              <span className="live-chip">Auto-refreshing</span>
            </div>
            <RescueMap
              emergencies={emergencies}
              selectedId={null}
              onSelect={handleSelect}
            />
          </section>
          <SafeZoneCard />
        </div>
      </div>
    </div>
  );
}
