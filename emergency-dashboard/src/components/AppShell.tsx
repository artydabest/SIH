import { useState } from "react";
import { Outlet } from "react-router-dom";
import Header from "./Header";
import Sidebar from "./Sidebar";
import Toasts from "./Toasts";
import AlertBanner from "./AlertBanner";
import { useEmergencyData } from "../context/EmergencyDataContext";
import "../styles/shell.css";

export default function AppShell() {
  const [collapsed, setCollapsed] = useState(false);
  const { emergencies, loading, connection, activeAlert } = useEmergencyData();

  const showOfflineBanner =
    connection === "unavailable" && !loading && emergencies.length === 0;

  return (
    <div className="app-shell">
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((c) => !c)} />
      <div className="app-shell__main">
        <Header />
        {activeAlert && <AlertBanner alert={activeAlert} />}
        {showOfflineBanner && (
          <div className="offline-banner" role="alert">
            <div>
              <strong>SYSTEM OFFLINE</strong>
              <p>Unable to connect to responder backend.</p>
            </div>
            <button type="button" className="btn btn--danger" onClick={() => window.location.reload()}>
              RETRY
            </button>
          </div>
        )}
        <main className="app-shell__content">
          <Outlet />
        </main>
      </div>
      <Toasts />
    </div>
  );
}
