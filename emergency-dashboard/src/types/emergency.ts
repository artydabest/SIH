export type EmergencyStatus =
  | "NEW"
  | "ACKNOWLEDGED"
  | "RESPONDING"
  | "RESOLVED";

export type ConfidenceLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface Emergency {
  _id: string;
  deviceId: string;
  latitude: number;
  longitude: number;
  altitude?: number | null;
  stationaryMinutes: number;
  nearbyDevices: number;
  emergencyMode?: boolean;
  /** Missing on records created before confidence scoring was added. */
  confidence?: number;
  confidenceLevel?: ConfidenceLevel;
  status: EmergencyStatus;
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
  NEW: "NEW ALERT",
  ACKNOWLEDGED: "ACKNOWLEDGED",
  RESPONDING: "RESPONDING",
  RESOLVED: "RESOLVED",
};
