export type WarningDisasterType =
  | "CYCLONE"
  | "FLOOD"
  | "HEAVY_RAINFALL"
  | "THUNDERSTORM"
  | "HEATWAVE"
  | "LIGHTNING"
  | "LANDSLIDE"
  | "EARTHQUAKE"
  | "TSUNAMI";

export type WarningSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface WarningInstructions {
  immediate: string[];
  avoid: string[];
  prepare: string[];
}

export interface SimWarning {
  _id: string;
  source: string;
  type: WarningDisasterType;
  severity: WarningSeverity;
  title: string;
  description: string;
  regionId: string;
  regionName: string;
  latitude: number;
  longitude: number;
  radiusKm: number;
  issuedAt: string;
  expectedStartAt: string;
  expectedEndAt: string;
  instructions: WarningInstructions;
  status: "ACTIVE" | "EXPIRED" | "CANCELLED";
  /** Computed server-side: status ACTIVE and not past expectedEndAt. */
  active: boolean;
}

export interface SimRegion {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  radiusKm: number;
  plausibleTypes: WarningDisasterType[];
  geoNote: string;
}

export const WARNING_TYPE_LABEL: Record<WarningDisasterType, string> = {
  CYCLONE: "Cyclone",
  FLOOD: "Flood",
  HEAVY_RAINFALL: "Heavy Rainfall",
  THUNDERSTORM: "Thunderstorm",
  HEATWAVE: "Heatwave",
  LIGHTNING: "Lightning",
  LANDSLIDE: "Landslide",
  EARTHQUAKE: "Earthquake",
  TSUNAMI: "Tsunami",
};

export const WARNING_SEVERITIES: WarningSeverity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

/** Severity → CSS modifier. Same palette family as incident status badges. */
export const SEVERITY_CLASS: Record<WarningSeverity, string> = {
  LOW: "low",
  MEDIUM: "medium",
  HIGH: "high",
  CRITICAL: "critical",
};
