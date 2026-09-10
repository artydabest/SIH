import { useEmergencyData } from "../context/EmergencyDataContext";
import "../styles/page.css";

export default function Settings() {
  const { backendUrl, dataSource } = useEmergencyData();

  return (
    <div className="page">
      <div className="page__heading">
        <h2>Settings</h2>
        <p className="page__sub">Operational configuration</p>
      </div>

      <div className="panel settings-panel">
        <dl className="fact-grid">
          <div className="fact">
            <dt>Backend URL</dt>
            <dd className="mono">{backendUrl}</dd>
          </div>
          <div className="fact">
            <dt>Configured via</dt>
            <dd className="mono">VITE_API_URL (.env.local)</dd>
          </div>
          <div className="fact">
            <dt>Current data source</dt>
            <dd>{dataSource === "live" ? "Live backend" : "Demo data (backend unreachable)"}</dd>
          </div>
          <div className="fact">
            <dt>Polling interval</dt>
            <dd className="mono">7 s</dd>
          </div>
        </dl>
        <p className="settings-note">
          Mock demo data can be disabled with{" "}
          <code className="mono">VITE_ENABLE_MOCK_DATA=false</code> in{" "}
          <code className="mono">.env.local</code>.
        </p>
      </div>
    </div>
  );
}
