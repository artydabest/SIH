export type SafeZoneKind = "SHELTER" | "HOSPITAL" | "RELIEF_CAMP" | "SUPPLY_DEPOT";

export interface SafeZone {
  _id: string;
  name: string;
  latitude: number;
  longitude: number;
  capacity?: number;
  kind: SafeZoneKind;
}

export const SAFE_ZONE_KIND_LABEL: Record<SafeZoneKind, string> = {
  SHELTER: "Shelter",
  HOSPITAL: "Hospital",
  RELIEF_CAMP: "Relief camp",
  SUPPLY_DEPOT: "Supply depot",
};
