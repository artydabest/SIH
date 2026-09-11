import mongoose from "mongoose";

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

    capacity: {
      type: Number,
      required: false,
    },

    kind: {
      type: String,
      enum: ["SHELTER", "HOSPITAL", "RELIEF_CAMP", "SUPPLY_DEPOT"],
      default: "SHELTER",
    },
  },
  {
    timestamps: true,
  }
);

export default mongoose.model("SafeZone", safeZoneSchema);
