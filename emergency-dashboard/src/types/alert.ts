export type DisasterType =
  | "EARTHQUAKE"
  | "FLOOD"
  | "CYCLONE"
  | "FIRE"
  | "LANDSLIDE"
  | "OTHER";

export interface DisasterAlert {
  _id: string;
  type: DisasterType;
  message: string;
  latitude?: number | null;
  longitude?: number | null;
  radiusKm?: number | null;
  active: boolean;
  createdAt: string;
  instructions?: string[];
}

export const DISASTER_LABEL: Record<DisasterType, string> = {
  EARTHQUAKE: "Earthquake",
  FLOOD: "Flood",
  CYCLONE: "Cyclone",
  FIRE: "Fire",
  LANDSLIDE: "Landslide",
  OTHER: "General emergency",
};
