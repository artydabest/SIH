import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import type { Emergency, EmergencyStatus } from "../types/emergency";
import type { Person } from "../types/person";
import type { SafeZone } from "../types/safezone";
import type { Volunteer } from "../types/volunteer";
import type { DisasterAlert } from "../types/alert";
import type { SimWarning } from "../types/warning";
import { getEmergencies, updateEmergencyStatus, API_URL } from "../api/emergencyApi";
import { getPeople } from "../api/peopleApi";
import { getSafeZones } from "../api/safezoneApi";
import { getVolunteers } from "../api/volunteerApi";
import { getWarnings, cancelWarning } from "../api/warningApi";
import { connectLive } from "../api/live";
import type { Toast } from "../types/toast";

const POLL_INTERVAL_MS = 7000;

export type ConnectionStatus = "connected" | "unavailable" | "unknown";

export interface EmergencyData {
  emergencies: Emergency[];
  people: Person[];
  safeZones: SafeZone[];
  volunteers: Volunteer[];
  activeAlert: DisasterAlert | null;
  warnings: SimWarning[];
  loading: boolean;
  refreshing: boolean;
  connection: ConnectionStatus;
  liveConnected: boolean;
  lastSync: number | null;
  refresh: () => void;
  updateStatus: (id: string, nextStatus: EmergencyStatus) => Promise<void>;
  updatingId: string | null;
  /** Withdraw a simulated warning (simulator demo control). */
  withdrawWarning: (id: string) => Promise<void>;
  backendUrl: string;
}

const EmergencyDataContext = createContext<EmergencyData | null>(null);

interface ProviderProps {
  children: ReactNode;
  onToast?: (toast: Toast) => void;
}

export function EmergencyDataProvider({ children, onToast }: ProviderProps) {
  const [emergencies, setEmergencies] = useState<Emergency[]>([]);
  const [people, setPeople] = useState<Person[]>([]);
  const [safeZones, setSafeZones] = useState<SafeZone[]>([]);
  const [volunteers, setVolunteers] = useState<Volunteer[]>([]);
  const [activeAlert, setActiveAlert] = useState<DisasterAlert | null>(null);
  const [warnings, setWarnings] = useState<SimWarning[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [connection, setConnection] = useState<ConnectionStatus>("unknown");
  const [liveConnected, setLiveConnected] = useState(false);
  const [lastSync, setLastSync] = useState<number | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const nextToastId = useRef(1);
  const pollTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const requestSeq = useRef(0);
  const announcedOutage = useRef(false);
  const knownIds = useRef<Set<string>>(new Set());

  const toast = useCallback(
    (t: Omit<Toast, "id">) => {
      onToast?.({ ...t, id: nextToastId.current++ });
    },
    [onToast]
  );

  const fetchEmergencies = useCallback(async () => {
    const seq = ++requestSeq.current;
    try {
      const data = await getEmergencies();
      if (seq !== requestSeq.current) return; // stale response — ignore

      setEmergencies(() => {
        if (knownIds.current.size > 0) {
          const fresh = data.filter((e) => !knownIds.current.has(e._id));
          if (fresh.length > 0) {
            toast({
              message: `${fresh.length} new incident${fresh.length === 1 ? "" : "s"} received`,
              kind: "info",
            });
          }
        }
        knownIds.current = new Set(data.map((e) => e._id));
        return data;
      });

      setConnection("connected");
      setLastSync(Date.now());
      announcedOutage.current = false;
    } catch {
      if (seq !== requestSeq.current) return;
      setConnection("unavailable");
      // Keep last real data visible during an outage; no fake data.
      if (!announcedOutage.current) {
        announcedOutage.current = true;
        toast({
          message: `Backend unavailable — unable to connect to ${API_URL}`,
          kind: "error",
        });
      }
    } finally {
      if (seq === requestSeq.current) {
        setLoading(false);
        setRefreshing(false);
      }
    }
  }, [toast]);

  // Single polling interval, cleaned up on unmount. Stale-response guarded.
  useEffect(() => {
    void fetchEmergencies();
    pollTimer.current = setInterval(() => {
      void fetchEmergencies();
    }, POLL_INTERVAL_MS);
    return () => {
      if (pollTimer.current !== null) {
        clearInterval(pollTimer.current);
        pollTimer.current = null;
      }
    };
  }, [fetchEmergencies]);

  // Auxiliary layers: people, safe zones, volunteers, active alert, warnings.
  const fetchAuxData = useCallback(async () => {
    const [peopleRes, zonesRes, volunteersRes, alertsRes, warningsRes] =
      await Promise.allSettled([
        getPeople(),
        getSafeZones(),
        getVolunteers(),
        fetch(`${API_URL}/api/alerts?active=1`).then((r) => {
          if (!r.ok) throw new Error("alerts fetch failed");
          return r.json() as Promise<{ success: boolean; alerts: DisasterAlert[] }>;
        }),
        getWarnings(),
      ]);

    if (peopleRes.status === "fulfilled") setPeople(peopleRes.value);
    if (zonesRes.status === "fulfilled") setSafeZones(zonesRes.value);
    if (volunteersRes.status === "fulfilled") setVolunteers(volunteersRes.value);
    if (alertsRes.status === "fulfilled") {
      const latest = alertsRes.value.alerts[0] ?? null;
      setActiveAlert(latest && latest.active ? latest : null);
    }
    if (warningsRes.status === "fulfilled") setWarnings(warningsRes.value);
  }, []);

  useEffect(() => {
    void fetchAuxData();
    const timer = setInterval(() => {
      void fetchAuxData();
    }, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [fetchAuxData]);

  // Live Socket.IO updates: patch state in place between polls.
  useEffect(() => {
    const socket = connectLive(
      API_URL,
      (event) => {
        if (event.type === "alert") {
          setActiveAlert(event.alert);
          toast({
            message: `${event.alert.type} ALERT — ${event.alert.message}`,
            kind: "error",
          });
        } else if (event.type === "warning") {
          setWarnings((current) =>
            current.some((w) => w._id === event.warning._id)
              ? current.map((w) => (w._id === event.warning._id ? event.warning : w))
              : [event.warning, ...current]
          );
          toast({
            message: `Simulated ${event.warning.severity} warning — ${event.warning.title}`,
            kind: event.warning.severity === "CRITICAL" ? "error" : "info",
          });
        } else if (event.type === "warning-update") {
          setWarnings((current) =>
            current.some((w) => w._id === event.warning._id)
              ? current.map((w) => (w._id === event.warning._id ? event.warning : w))
              : current
          );
        } else if (event.type === "new") {
          setEmergencies((current) =>
            current.some((e) => e._id === event.emergency._id)
              ? current
              : [event.emergency, ...current]
          );
          knownIds.current.add(event.emergency._id);
          toast({
            message: `New incident from device #${event.emergency.deviceId}`,
            kind: "info",
          });
        } else {
          setEmergencies((current) =>
            current.map((e) =>
              e._id === event.emergency._id ? event.emergency : e
            )
          );
        }
        void fetchAuxData();
        setConnection("connected");
        setLastSync(Date.now());
      },
      (connected) => setLiveConnected(connected)
    );

    return () => {
      socket.disconnect();
    };
  }, [toast, fetchAuxData]);

  const refresh = useCallback(() => {
    setRefreshing(true);
    void fetchEmergencies();
  }, [fetchEmergencies]);

  const updateStatus = useCallback(
    async (id: string, nextStatus: EmergencyStatus) => {
      setUpdatingId(id);
      try {
        await updateEmergencyStatus(id, nextStatus);
        toast({ message: `Incident acknowledged — status ${nextStatus}`, kind: "info" });
        await fetchEmergencies();
      } catch (error) {
        const message = error instanceof Error ? error.message : "Unknown error";
        toast({ message: `Failed to update status: ${message}`, kind: "error" });
      } finally {
        setUpdatingId(null);
      }
    },
    [fetchEmergencies, toast]
  );

  const withdrawWarning = useCallback(
    async (id: string) => {
      try {
        await cancelWarning(id);
        setWarnings((current) =>
          current.map((w) => (w._id === id ? { ...w, status: "CANCELLED" as const, active: false } : w))
        );
        toast({ message: "Warning withdrawn", kind: "info" });
      } catch (error) {
        const message = error instanceof Error ? error.message : "Unknown error";
        toast({ message: `Failed to withdraw warning: ${message}`, kind: "error" });
      }
    },
    [toast]
  );

  const value = useMemo<EmergencyData>(
    () => ({
      emergencies,
      people,
      safeZones,
      volunteers,
      activeAlert,
      warnings,
      loading,
      refreshing,
      connection,
      liveConnected,
      lastSync,
      refresh,
      updateStatus,
      updatingId,
      withdrawWarning,
      backendUrl: API_URL,
    }),
    [
      emergencies,
      people,
      safeZones,
      volunteers,
      activeAlert,
      warnings,
      loading,
      refreshing,
      connection,
      liveConnected,
      lastSync,
      refresh,
      updateStatus,
      updatingId,
      withdrawWarning,
    ]
  );

  return (
    <EmergencyDataContext.Provider value={value}>
      {children}
    </EmergencyDataContext.Provider>
  );
}

export function useEmergencyData(): EmergencyData {
  const ctx = useContext(EmergencyDataContext);
  if (!ctx) {
    throw new Error("useEmergencyData must be used within EmergencyDataProvider");
  }
  return ctx;
}
