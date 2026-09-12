import mongoose from "mongoose";

/** Facilities a shelter can offer (data-driven, rendered by clients). */
export const SHELTER_FACILITIES = [
  "MEDICAL_ASSISTANCE",
  "DRINKING_WATER",
  "FOOD",
  "TOILETS",
  "CHARGING",
  "FIRST_AID",
  "TEMPORARY_ACCOMMODATION",
  "EMERGENCY_SUPPLIES",
] as const;

export type ShelterFacility = (typeof SHELTER_FACILITIES)[number];

/** Disaster types a shelter can be suitable for (subset of warning types). */
export const SHELTER_SUITABILITY = [
  "FLOOD",
  "HEAVY_RAINFALL",
  "CYCLONE",
  "THUNDERSTORM",
  "LIGHTNING",
  "HEATWAVE",
  "LANDSLIDE",
  "EARTHQUAKE",
  "TSUNAMI",
] as const;

export type ShelterSuitability = (typeof SHELTER_SUITABILITY)[number];

const safeZoneSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
    },

    latitude: {
      type: Number,
      required: true,
    },

    longitude: {
      type: Number,
      required: true,
    },

    kind: {
      type: String,
      enum: ["SHELTER", "HOSPITAL", "RELIEF_CAMP", "SUPPLY_DEPOT"],
      default: "SHELTER",
    },

    /** Total people the site can accommodate; null when unknown. */
    capacity: {
      type: Number,
      required: false,
    },

    /** Simulated current occupancy for the demo; never presented as live truth. */
    currentOccupancy: {
      type: Number,
      required: false,
      default: 0,
    },

    /** Facilities available on site (see SHELTER_FACILITIES). */
    facilities: {
      type: [String],
      enum: SHELTER_FACILITIES,
      default: [],
    },

    /** Disaster types this site is suitable for (see SHELTER_SUITABILITY). */
    suitableFor: {
      type: [String],
      enum: SHELTER_SUITABILITY,
      default: [],
    },

    /** Wheelchair accessible entrances and areas. */
    accessible: {
      type: Boolean,
      default: false,
    },

    /** Operating status; CLOSED sites are never recommended. */
    operatingStatus: {
      type: String,
      enum: ["OPEN", "FULL", "CLOSED"],
      default: "OPEN",
    },

    /** When the shelter info was last verified by a volunteer/authority. */
    lastVerifiedAt: {
      type: Date,
      required: false,
    },
  },
  {
    timestamps: true,
  }
);

export interface SafeZoneDoc extends mongoose.Document {
  name: string;
  latitude: number;
  longitude: number;
  kind: "SHELTER" | "HOSPITAL" | "RELIEF_CAMP" | "SUPPLY_DEPOT";
  capacity?: number;
  currentOccupancy: number;
  facilities: ShelterFacility[];
  suitableFor: ShelterSuitability[];
  accessible: boolean;
  operatingStatus: "OPEN" | "FULL" | "CLOSED";
  lastVerifiedAt?: Date;
}

export default mongoose.model<SafeZoneDoc>("SafeZone", safeZoneSchema);
