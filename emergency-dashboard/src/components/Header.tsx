import { RefreshCw, Radio } from "lucide-react";
import { useEmergencyData } from "../context/EmergencyDataContext";
import { useNow } from "../hooks/useNow";
import { formatRelative } from "../lib/format";
import "../styles/header.css";

export default function Header() {
  const { connection, lastSync, refresh, refreshing, liveConnected } =
    useEmergencyData();
  const now = useNow(1000);

  const online = connection === "connected";
  const statusText = online
    ? "SYSTEM ONLINE"
    : connection === "unavailable"
      ? "SYSTEM OFFLINE"
      : "CONNECTING…";

  return (
    <header className="ops-header">
      <div className="ops-header__brand">
        <span className="ops-header__logo" aria-hidden="true">
          <Radio size={20} strokeWidth={2.2} />
        </span>
        <div>
          <h1>EMERGENCY RESPONSE</h1>
          <p>Responder Operations</p>
        </div>
      </div>

      <div className="ops-header__status">
        <span
          className={`conn-pill ${online ? "conn-pill--online" : "conn-pill--offline"}`}
          role="status"
          aria-live="polite"
        >
          <span className="conn-pill__dot" aria-hidden="true" />
          {statusText}
        </span>
        {liveConnected && <span className="live-chip">⚡ LIVE</span>}
        <span className="ops-header__sync mono">
          {lastSync ? `Last sync ${formatRelative(new Date(lastSync).toISOString(), now.getTime())}` : "Awaiting first sync"}
        </span>
      </div>

      <div className="ops-header__right">
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
          {refreshing ? "SYNCING" : "REFRESH"}
        </button>
      </div>
    </header>
  );
}
