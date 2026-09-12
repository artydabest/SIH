/**
 * Data-driven disaster instruction catalog.
 * The backend attaches these to every warning so clients never hardcode
 * per-disaster UI logic — they render whatever the payload contains.
 */

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

export interface DisasterInstructions {
  /** What to do the moment the warning/event becomes relevant. */
  immediate: string[];
  /** Things to avoid during the event. */
  avoid: string[];
  /** How to prepare while there is still time (advance warnings). */
  prepare: string[];
}

export const DISASTER_INSTRUCTIONS: Record<
  WarningDisasterType,
  DisasterInstructions
> = {
  EARTHQUAKE: {
    immediate: [
      "DROP to the ground, take COVER under a sturdy table or desk",
      "HOLD ON until the shaking stops",
      "Stay away from windows, glass, and falling objects",
      "If outdoors, move away from buildings, trees, and power lines",
    ],
    avoid: [
      "Do NOT use elevators",
      "Do NOT run outside while the shaking continues",
      "Avoid damaged buildings after the shaking stops",
    ],
    prepare: [
      "Identify safe spots in each room (under sturdy tables)",
      "Keep shoes and a torch within reach",
      "Secure heavy furniture and shelves to walls",
    ],
  },
  FLOOD: {
    immediate: [
      "Move immediately to higher ground — do NOT wait",
      "Follow evacuation routes to the nearest safe zone on your map",
      "Keep your phone charged and with you",
    ],
    avoid: [
      "Never walk or drive through moving water",
      "Avoid contact with floodwater — it may be electrically live",
      "Do not return home until authorities declare it safe",
    ],
    prepare: [
      "Move vehicles and valuables off low-lying streets",
      "Keep documents in waterproof bags",
      "Stock drinking water and dry food for 3 days",
    ],
  },
  HEAVY_RAINFALL: {
    immediate: [
      "Move indoors and stay off the roads if possible",
      "Shift to upper floors if your area floods",
      "Follow evacuation routes to the nearest safe zone if water rises",
    ],
    avoid: [
      "Avoid low-lying roads and underpasses",
      "Avoid unnecessary travel",
      "Stay away from electric poles and fallen lines",
    ],
    prepare: [
      "Keep emergency supplies ready",
      "Charge phones and power banks",
      "Move away from flood-prone areas while roads are open",
    ],
  },
  THUNDERSTORM: {
    immediate: [
      "Move indoors immediately",
      "Unplug non-essential electrical appliances",
      "Stay away from windows and skylights",
    ],
    avoid: [
      "Do not shelter under isolated trees",
      "Avoid open fields, rooftops, and water bodies",
      "Do not touch wired devices during the storm",
    ],
    prepare: [
      "Secure outdoor furniture and loose objects",
      "Park vehicles away from trees",
      "Keep a torch handy in case of power cuts",
    ],
  },
  HEATWAVE: {
    immediate: [
      "Move to a cool or shaded area",
      "Drink water even if you do not feel thirsty",
      "Rest during the hottest hours of the day",
    ],
    avoid: [
      "Avoid outdoor activity between 12:00 and 16:00",
      "Avoid alcohol, caffeine, and heavy meals",
      "Never leave children or pets inside parked vehicles",
    ],
    prepare: [
      "Check on elderly neighbours and vulnerable people",
      "Keep ORS and drinking water stocked",
      "Wear light-coloured, loose clothing when outside",
    ],
  },
  LIGHTNING: {
    immediate: [
      "Move indoors or into a hard-topped vehicle",
      "Keep away from windows and metal objects",
      "Disconnect sensitive electrical equipment if safe to do so",
    ],
    avoid: [
      "Avoid open fields, hilltops, and rooftops",
      "Avoid isolated trees and metal fences",
      "Do not bathe or use running water during strikes",
    ],
    prepare: [
      "Track the storm's approach and plan indoor shelter",
      "Charge devices in advance",
      "Know the 30-30 rule: if thunder follows lightning within 30 seconds, go indoors",
    ],
  },
  LANDSLIDE: {
    immediate: [
      "Move away from slopes and steep cuttings if warned",
      "Follow evacuation instructions from local authorities",
      "Alert others in the path of the slope",
    ],
    avoid: [
      "Do not approach debris zones or damaged slopes",
      "Avoid river valleys and steep road cuttings",
      "Listen for cracking trees and rumbling sounds",
    ],
    prepare: [
      "Plan routes that avoid unstable roads",
      "Keep an evacuation bag ready",
      "Note the nearest elevated shelter on your map",
    ],
  },
  CYCLONE: {
    immediate: [
      "Stay indoors, away from windows and glass doors",
      "Take shelter in an interior room on the lowest floor",
      "Keep your emergency kit within reach",
    ],
    avoid: [
      "Do not go outside even if there is a calm lull — winds return",
      "Do not touch fallen power lines",
      "Wait for the official all-clear before moving",
    ],
    prepare: [
      "Secure loose objects on balconies and rooftops",
      "Store drinking water for several days",
      "Follow evacuation orders for low-lying coastal areas",
    ],
  },
  TSUNAMI: {
    immediate: [
      "Move inland and uphill immediately — do NOT wait to see the water",
      "Follow evacuation routes away from the shoreline",
      "Alert others near the coast",
    ],
    avoid: [
      "Never go to the beach to watch the waves",
      "Do not return until authorities declare the all-clear",
      "Avoid river mouths and harbours",
    ],
    prepare: [
      "Know your nearest high ground and route to it",
      "Keep documents and medicines in a go-bag",
      "Arrange a family meeting point inland",
    ],
  },
};

export interface DisasterRegion {
  /** Stable key used by seeds and the simulator's region picker. */
  id: string;
  name: string;
  /** Approximate centroid of the affected area. */
  latitude: number;
  longitude: number;
  /** Affected-area radius in km (drives location relevance checks). */
  radiusKm: number;
  /** Geographically plausible disaster types for this region. */
  plausibleTypes: WarningDisasterType[];
  /** Short geo rationale, shown in the simulator so the demo reads honestly. */
  geoNote: string;
}

/**
 * Simulated regions for the demo. Geographically sensible: Bengaluru never
 * gets tsunami/earthquake warnings; coasts get cyclone/tsunami; the Western
 * Ghats get landslide/heavy rainfall warnings.
 */
export const SIMULATED_REGIONS: DisasterRegion[] = [
  {
    id: "bengaluru-urban",
    name: "Bengaluru Urban",
    latitude: 12.9716,
    longitude: 77.5946,
    radiusKm: 25,
    plausibleTypes: ["HEAVY_RAINFALL", "THUNDERSTORM", "LIGHTNING", "HEATWAVE", "FLOOD"],
    geoNote: "Deccan plateau city — rainfall, storms, heat, urban flooding",
  },
  {
    id: "odisha-coast",
    name: "Odisha Coast (Puri)",
    latitude: 19.8135,
    longitude: 85.8312,
    radiusKm: 60,
    plausibleTypes: ["CYCLONE", "FLOOD", "THUNDERSTORM"],
    geoNote: "Bay of Bengal coastline — cyclones and storm surge",
  },
  {
    id: "chennai",
    name: "Chennai",
    latitude: 13.0827,
    longitude: 80.2707,
    radiusKm: 35,
    plausibleTypes: ["CYCLONE", "FLOOD", "HEAVY_RAINFALL"],
    geoNote: "Coastal metro with a history of monsoon flooding",
  },
  {
    id: "kerala-western-ghats",
    name: "Wayanad, Western Ghats",
    latitude: 11.6854,
    longitude: 76.132,
    radiusKm: 40,
    plausibleTypes: ["LANDSLIDE", "HEAVY_RAINFALL"],
    geoNote: "Hilly terrain — landslides during intense monsoon rain",
  },
  {
    id: "north-bihar",
    name: "North Bihar (Muzaffarpur)",
    latitude: 26.1209,
    longitude: 85.3647,
    radiusKm: 55,
    plausibleTypes: ["FLOOD", "HEAVY_RAINFALL"],
    geoNote: "Ganges floodplain — annual monsoon river flooding",
  },
  {
    id: "mumbai-coastal",
    name: "Mumbai",
    latitude: 19.076,
    longitude: 72.8777,
    radiusKm: 30,
    plausibleTypes: ["HEAVY_RAINFALL", "FLOOD", "THUNDERSTORM"],
    geoNote: "Coastal city — extreme monsoon rain and high tide flooding",
  },
  {
    id: "delhi-ncr",
    name: "Delhi NCR",
    latitude: 28.6139,
    longitude: 77.209,
    radiusKm: 35,
    plausibleTypes: ["HEATWAVE", "THUNDERSTORM", "LIGHTNING"],
    geoNote: "Inland northern plains — summer heatwaves, pre-monsoon storms",
  },
  {
    id: "visakhapatnam-coast",
    name: "Visakhapatnam Coast",
    latitude: 17.6868,
    longitude: 83.2185,
    radiusKm: 45,
    plausibleTypes: ["CYCLONE", "TSUNAMI", "FLOOD"],
    geoNote: "East-coast port city — cyclone and tsunami exposure",
  },
];

export function findRegion(id: string): DisasterRegion | undefined {
  return SIMULATED_REGIONS.find((region) => region.id === id);
}
