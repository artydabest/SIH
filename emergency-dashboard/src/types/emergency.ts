export type EmergencyStatus =
  | "NEW"
  | "ACKNOWLEDGED"
  | "RESPONDING"
  | "RESOLVED";

/** Backend-computed confidence classification (optional — not on older docs). */
export type ConfidenceLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface Emergency {
  _id: string;
  deviceId: string;
  latitude: number;
  longitude: number;
  altitude?: number | null;
  stationaryMinutes: number;
  nearbyDevices: number;
  status: EmergencyStatus;
  /** 0–100, computed by the backend when available. */
  confidence?: number;
  confidenceLevel?: ConfidenceLevel;
  createdAt: string;
  updatedAt: string;
}

/** Next logical status in the responder workflow, or null when terminal. */
export const NEXT_STATUS: Record<EmergencyStatus, EmergencyStatus | null> = {
  NEW: "ACKNOWLEDGED",
  ACKNOWLEDGED: "RESPONDING",
  RESPONDING: "RESOLVED",
  RESOLVED: null,
};

/** Operational action label for the next workflow step. */
export const ACTION_LABEL: Record<EmergencyStatus, string | null> = {
  NEW: "Acknowledge Incident",
  ACKNOWLEDGED: "Start Response",
  RESPONDING: "Mark Resolved",
  RESOLVED: null,
};

export const STATUS_LABEL: Record<EmergencyStatus, string> = {
  NEW: "New alert",
  ACKNOWLEDGED: "Acknowledged",
  RESPONDING: "Responding",
  RESOLVED: "Resolved",
};
