import mongoose from "mongoose";
import {
  DISASTER_INSTRUCTIONS,
  type DisasterInstructions,
  type WarningDisasterType,
  type WarningSeverity,
} from "../data/disasterCatalog";

const warningSchema = new mongoose.Schema(
  {
    /** Simulated source label; never claims a real meteorological agency. */
    source: {
      type: String,
      required: true,
      default: "Meteorological Alert Simulator (DEMO)",
    },
    type: {
      type: String,
      required: true,
      enum: Object.keys(DISASTER_INSTRUCTIONS),
    },
    severity: {
      type: String,
      required: true,
      enum: ["LOW", "MEDIUM", "HIGH", "CRITICAL"],
    },
    title: {
      type: String,
      required: true,
      trim: true,
    },
    description: {
      type: String,
      required: true,
      trim: true,
    },
    regionId: {
      type: String,
      required: true,
    },
    regionName: {
      type: String,
      required: true,
    },
    latitude: {
      type: Number,
      required: true,
    },
    longitude: {
      type: Number,
      required: true,
    },
    radiusKm: {
      type: Number,
      required: true,
    },
    issuedAt: {
      type: Date,
      required: true,
      default: Date.now,
    },
    /** When the hazard is expected to begin — enables advance warnings. */
    expectedStartAt: {
      type: Date,
      required: true,
    },
    /** When the hazard is expected to end; past this the warning auto-expires. */
    expectedEndAt: {
      type: Date,
      required: true,
    },
    /** Snapshot of the catalog instructions at issue time. */
    instructions: {
      immediate: { type: [String], default: [] },
      avoid: { type: [String], default: [] },
      prepare: { type: [String], default: [] },
    },
    status: {
      type: String,
      enum: ["ACTIVE", "EXPIRED", "CANCELLED"],
      default: "ACTIVE",
    },
  },
  {
    timestamps: true,
  }
);

/** A warning is displayable while issued, not cancelled, and not past its end. */
warningSchema.methods.isActive = function isActive(): boolean {
  return this.status === "ACTIVE" && this.expectedEndAt.getTime() > Date.now();
};

export interface WarningDoc extends mongoose.Document {
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
  issuedAt: Date;
  expectedStartAt: Date;
  expectedEndAt: Date;
  instructions: DisasterInstructions;
  status: "ACTIVE" | "EXPIRED" | "CANCELLED";
  isActive(): boolean;
}

export default mongoose.model<WarningDoc>("Warning", warningSchema);
