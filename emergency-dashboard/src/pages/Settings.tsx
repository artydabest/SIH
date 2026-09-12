import { useState } from "react";
import { useEmergencyData } from "../context/EmergencyDataContext";
import { triggerAlert } from "../api/alertApi";
import type { DisasterType } from "../types/alert";
import { DISASTER_LABEL } from "../types/alert";
import "../styles/page.css";

const DRILL_TYPES: DisasterType[] = [
  "EARTHQUAKE",
  "FLOOD",
  "CYCLONE",
  "FIRE",
  "LANDSLIDE",
  "OTHER",
];

export default function Settings() {
  const { backendUrl } = useEmergencyData();
  const [sending, setSending] = useState<DisasterType | null>(null);
  const [lastResult, setLastResult] = useState<string | null>(null);

  const runDrill = async (type: DisasterType) => {
    setSending(type);
    setLastResult(null);
    try {
      await triggerAlert(type, { radiusKm: 5 });
      setLastResult(`${DISASTER_LABEL[type]} drill broadcast to all apps + dashboards.`);
    } catch (error) {
      setLastResult(error instanceof Error ? error.message : "Drill failed");
    } finally {
      setSending(null);
    }
  };

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
            <dt>Polling interval</dt>
            <dd className="mono">7 s</dd>
          </div>
        </dl>
        <p className="settings-note">
          All data shown is live from the backend — safe zones, volunteers, and
          friend check-ins included.
        </p>
      </div>

      <div className="panel settings-panel" style={{ marginTop: "1rem" }}>
        <h3 className="panel__title">DISASTER DRILL — NOTIFY ALL USERS</h3>
        <p className="muted" style={{ fontSize: "0.8rem", margin: "0.5rem 0 0.9rem" }}>
          Sends an instant alert to every citizen app (popup + phone
          notification) and dashboard with safety instructions for the
          selected disaster.
        </p>
        <div className="drill-grid">
          {DRILL_TYPES.map((type) => (
            <button
              key={type}
              type="button"
              className="btn btn--danger"
              disabled={sending !== null}
              onClick={() => void runDrill(type)}
            >
              {sending === type ? "SENDING…" : DISASTER_LABEL[type].toUpperCase()}
            </button>
          ))}
        </div>
        {lastResult && <p className="settings-note" style={{ marginTop: "0.8rem" }}>{lastResult}</p>}
      </div>
    </div>
  );
}
