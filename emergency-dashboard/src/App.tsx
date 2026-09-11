import { BrowserRouter, Route, Routes } from "react-router-dom";
import { useCallback, type ReactNode } from "react";
import { ToastProvider, useToasts } from "./context/ToastContext";
import type { Toast } from "./types/toast";
import { EmergencyDataProvider } from "./context/EmergencyDataContext";
import ErrorBoundary from "./components/ErrorBoundary";
import AppShell from "./components/AppShell";
import Dashboard from "./pages/Dashboard";
import Incidents from "./pages/Incidents";
import IncidentDetailPage from "./pages/IncidentDetailPage";
import MapPage from "./pages/MapPage";
import History from "./pages/History";
import People from "./pages/People";
import Settings from "./pages/Settings";
import "./styles/tokens.css";

/** Bridges data-layer events (outage warnings, status confirmations) into toasts. */
function EmergencyDataBridge({ children }: { children: ReactNode }) {
  const { pushToast } = useToasts();
  const handleToast = useCallback(
    (toast: Toast) => pushToast(toast.message, toast.kind),
    [pushToast]
  );
  return (
    <EmergencyDataProvider onToast={handleToast}>
      {children}
    </EmergencyDataProvider>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <ToastProvider>
        <EmergencyDataBridge>
          <BrowserRouter>
            <Routes>
              <Route element={<AppShell />}>
                <Route index element={<Dashboard />} />
                <Route path="incidents" element={<Incidents />} />
                <Route path="incidents/:id" element={<IncidentDetailPage />} />
                <Route path="map" element={<MapPage />} />
                <Route path="history" element={<History />} />
                <Route path="people" element={<People />} />
                <Route path="settings" element={<Settings />} />
                <Route path="*" element={<Dashboard />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </EmergencyDataBridge>
      </ToastProvider>
    </ErrorBoundary>
  );
}
