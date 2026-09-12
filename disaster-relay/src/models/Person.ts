import mongoose from "mongoose";

const personSchema = new mongoose.Schema(
  {
    deviceId: {
      type: String,
      required: true,
      unique: true,
    },

    name: {
      type: String,
      required: false,
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

    /** User-declared status: true = "I am safe", false = "I need help". */
    safe: {
      type: Boolean,
      default: true,
    },

    lastSeenAt: {
      type: Date,
      required: true,
      default: Date.now,
    },
  },
  {
    timestamps: true,
  }
);

export default mongoose.model("Person", personSchema);
