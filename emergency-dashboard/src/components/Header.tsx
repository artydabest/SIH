import { RefreshCw, Radio } from "lucide-react";
import { useEmergencyData } from "../context/EmergencyDataContext";
import { useNow } from "../hooks/useNow";
import "../styles/header.css";

export default function Header() {
  const { refresh, refreshing, dataSource } = useEmergencyData();
  const now = useNow(1000);

  return (
    <header className="ops-header">
      <div className="ops-header__brand">
        <span className="ops-header__logo" aria-hidden="true">
          <Radio size={20} strokeWidth={2.2} />
        </span>
        <div>
          <h1>Emergency Response</h1>
          <p>Responder operations</p>
        </div>
      </div>

      {dataSource === "demo" && (
        <div className="ops-header__status">
          <span className="demo-chip" title="Backend unreachable — showing demo data">
            Demo data
          </span>
        </div>
      )}

      <div className="ops-header__right">
        <span className="ops-header__responder">Prady · Responder</span>
        <time className="ops-header__clock mono" dateTime={now.toISOString()}>
          {now.toLocaleTimeString()}
        </time>
        <button
          type="button"
          className="refresh-btn"
          onClick={refresh}
          disabled={refreshing}
          aria-label="Refresh incident data"
        >
          <RefreshCw size={14} className={refreshing ? "spin" : undefined} />
          {refreshing ? "Syncing…" : "Refresh"}
        </button>
      </div>
    </header>
  );
}
