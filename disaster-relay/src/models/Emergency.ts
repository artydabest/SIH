import mongoose from "mongoose";

const emergencySchema = new mongoose.Schema(
  {
    deviceId: {
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

    altitude: {
      type: Number,
      required: false,
    },

    stationaryMinutes: {
      type: Number,
      required: true,
    },

    nearbyDevices: {
      type: Number,
      required: true,
    },

    status: {
      type: String,
      enum: ["NEW", "ACKNOWLEDGED", "RESPONDING", "RESOLVED"],
      default: "NEW",
    },
  },
  {
    timestamps: true,
  }
);

export default mongoose.model("Emergency", emergencySchema);