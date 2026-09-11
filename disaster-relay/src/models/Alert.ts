import mongoose from "mongoose";

export type DisasterType =
  | "EARTHQUAKE"
  | "FLOOD"
  | "CYCLONE"
  | "FIRE"
  | "LANDSLIDE"
  | "OTHER";

const alertSchema = new mongoose.Schema(
  {
    type: {
      type: String,
      enum: ["EARTHQUAKE", "FLOOD", "CYCLONE", "FIRE", "LANDSLIDE", "OTHER"],
      required: true,
    },

    message: {
      type: String,
      required: true,
      trim: true,
    },

    latitude: {
      type: Number,
      required: false,
    },

    longitude: {
      type: Number,
      required: false,
    },

    /** Radius of the danger zone in km; no zone when omitted. */
    radiusKm: {
      type: Number,
      required: false,
    },

    active: {
      type: Boolean,
      default: true,
    },
  },
  {
    timestamps: true,
  }
);

/** Safety instructions per disaster type — the app displays these verbatim. */
export const SAFETY_INSTRUCTIONS: Record<DisasterType, string[]> = {
  EARTHQUAKE: [
    "DO NOT run outside while the shaking continues",
    "DROP to the ground, take COVER under a sturdy table or desk",
    "HOLD ON until the shaking stops",
    "Stay away from windows, glass, and falling objects",
    "If outdoors, move away from buildings, trees, and power lines",
    "After shaking stops, exit via stairs — never use elevators",
  ],
  FLOOD: [
    "Move immediately to higher ground — do NOT wait",
    "Never walk or drive through moving water",
    "Avoid contact with floodwater — it may be electrically live",
    "Keep your phone charged and with you",
    "Follow evacuation routes to the nearest safe zone on your map",
  ],
  CYCLONE: [
    "Stay indoors, away from windows and glass doors",
    "Take shelter in an interior room on the lowest floor",
    "Keep an emergency kit: water, torch, first aid, power bank",
    "Do not go outside even if there is a calm lull — winds return",
    "Wait for the official all-clear before moving",
  ],
  FIRE: [
    "Get low and move out of the building immediately",
    "Do NOT use elevators",
    "Feel doors with the back of your hand before opening — if hot, use another route",
    "Cover your nose and mouth with a cloth",
    "Once out, stay out — call emergency services",
  ],
  LANDSLIDE: [
    "Move away from the path of the landslide — run sideways, not down",
    "Avoid river valleys and steep slopes",
    "Listen for cracking trees and rumbling sounds",
    "Head to the nearest elevated safe zone on your map",
  ],
  OTHER: [
    "Stay calm and move to the nearest safe zone on your map",
    "Follow instructions from volunteers and authorities",
    "Keep your phone reachable",
    "Check on people around you",
  ],
};

export default mongoose.model("Alert", alertSchema);
