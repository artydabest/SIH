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
import { getEmergencies, updateEmergencyStatus, API_URL } from "../api/emergencyApi";
import { MOCK_EMERGENCIES } from "../mocks/demoEmergencies";
import type { Toast } from "../types/toast";

const POLL_INTERVAL_MS = 7000;

export type ConnectionStatus = "connected" | "unavailable" | "unknown";

export type DataSource = "live" | "demo";

export interface EmergencyData {
  emergencies: Emergency[];
  loading: boolean;
  refreshing: boolean;
  connection: ConnectionStatus;
  dataSource: DataSource;
  lastSync: number | null;
  refresh: () => void;
  updateStatus: (id: string, nextStatus: EmergencyStatus) => Promise<void>;
  updatingId: string | null;
  backendUrl: string;
}

const EmergencyDataContext = createContext<EmergencyData | null>(null);

const MOCK_ENABLED = import.meta.env.VITE_ENABLE_MOCK_DATA !== "false";

interface ProviderProps {
  children: ReactNode;
  onToast?: (toast: Toast) => void;
}

export function EmergencyDataProvider({ children, onToast }: ProviderProps) {
  const [emergencies, setEmergencies] = useState<Emergency[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [connection, setConnection] = useState<ConnectionStatus>("unknown");
  const [dataSource, setDataSource] = useState<DataSource>("live");
  const [lastSync, setLastSync] = useState<number | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const nextToastId = useRef(1);
  const pollTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const requestSeq = useRef(0);
  const announcedOutage = useRef(false);

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
      setEmergencies(data);
      setConnection("connected");
      setDataSource("live");
      setLastSync(Date.now());
      announcedOutage.current = false;
    } catch {
      if (seq !== requestSeq.current) return;
      setConnection("unavailable");
      // Demo mode: only when we have never received real data.
      setEmergencies((current) => {
        if (current.length === 0 && MOCK_ENABLED) {
          setDataSource("demo");
          return MOCK_EMERGENCIES;
        }
        return current; // keep last real data visible during outage
      });
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

  const value = useMemo<EmergencyData>(
    () => ({
      emergencies,
      loading,
      refreshing,
      connection,
      dataSource,
      lastSync,
      refresh,
      updateStatus,
      updatingId,
      backendUrl: API_URL,
    }),
    [
      emergencies,
      loading,
      refreshing,
      connection,
      dataSource,
      lastSync,
      refresh,
      updateStatus,
      updatingId,
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
