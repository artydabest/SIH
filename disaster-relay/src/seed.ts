import SafeZone from "./models/SafeZone";
import Volunteer from "./models/Volunteer";
import Warning from "./models/Warning";
import {
  DISASTER_INSTRUCTIONS,
  findRegion,
  type WarningDisasterType,
  type WarningSeverity,
} from "./data/disasterCatalog";

const SEED_SAFE_ZONES = [
  {
    name: "City Emergency Shelter",
    latitude: 12.9716,
    longitude: 77.5946,
    capacity: 500,
    currentOccupancy: 120,
    kind: "SHELTER",
    facilities: ["DRINKING_WATER", "FOOD", "TOILETS", "CHARGING", "FIRST_AID", "EMERGENCY_SUPPLIES"],
    suitableFor: ["FLOOD", "HEAVY_RAINFALL", "THUNDERSTORM", "LIGHTNING", "HEATWAVE"],
    accessible: true,
    operatingStatus: "OPEN",
    lastVerifiedAt: new Date(),
  },
  {
    name: "General Hospital",
    latitude: 12.9352,
    longitude: 77.6245,
    capacity: 200,
    currentOccupancy: 60,
    kind: "HOSPITAL",
    facilities: ["MEDICAL_ASSISTANCE", "FIRST_AID", "DRINKING_WATER", "CHARGING"],
    suitableFor: ["FLOOD", "HEAVY_RAINFALL", "EARTHQUAKE", "CYCLONE"],
    accessible: true,
    operatingStatus: "OPEN",
    lastVerifiedAt: new Date(),
  },
  {
    name: "Relief Camp North",
    latitude: 12.998,
    longitude: 77.58,
    capacity: 300,
    currentOccupancy: 300,
    kind: "RELIEF_CAMP",
    facilities: ["TEMPORARY_ACCOMMODATION", "FOOD", "TOILETS", "DRINKING_WATER"],
    suitableFor: ["FLOOD", "HEAVY_RAINFALL", "CYCLONE"],
    accessible: false,
    operatingStatus: "FULL",
    lastVerifiedAt: new Date(),
  },
  {
    name: "Supply Depot South",
    latitude: 12.9082,
    longitude: 77.6474,
    capacity: undefined,
    currentOccupancy: 0,
    kind: "SUPPLY_DEPOT",
    facilities: ["EMERGENCY_SUPPLIES", "DRINKING_WATER"],
    suitableFor: [],
    accessible: true,
    operatingStatus: "OPEN",
    lastVerifiedAt: new Date(),
  },
] as const;

const SEED_VOLUNTEERS = [
  { name: "Volunteer A", latitude: 12.9725, longitude: 77.592, available: true, phone: "+91-90000-00001" },
  { name: "Volunteer B", latitude: 12.94, longitude: 77.61, available: true, phone: "+91-90000-00002" },
  { name: "Volunteer C", latitude: 12.98, longitude: 77.65, available: false, phone: "+91-90000-00003" },
] as const;

/** Idempotent: only seeds collections that are still empty. */
export async function seedDatabase(): Promise<void> {
  const zoneCount = await SafeZone.estimatedDocumentCount();
  if (zoneCount === 0) {
    await SafeZone.insertMany(
      SEED_SAFE_ZONES.map((zone) => ({ ...zone }))
    );
    console.log(`Seeded ${SEED_SAFE_ZONES.length} safe zones`);
  } else {
    console.log(`Safe zones already seeded (${zoneCount})`);
  }

  const volunteerCount = await Volunteer.estimatedDocumentCount();
  if (volunteerCount === 0) {
    await Volunteer.insertMany(SEED_VOLUNTEERS.map((v) => ({ ...v })));
    console.log(`Seeded ${SEED_VOLUNTEERS.length} volunteers`);
  } else {
    console.log(`Volunteers already seeded (${volunteerCount})`);
  }
}

/**
 * Simulated meteorological warnings for demo. Geographically plausible:
 * each entry pairs a disaster type with a region where that type actually
 * occurs. Clearly labeled as simulated; never presented as real data.
 */
interface WarningSeed {
  type: WarningDisasterType;
  severity: WarningSeverity;
  regionId: string;
  title: string;
  description: string;
  /** Hours until expected start (advance warning). */
  startInHours: number;
  durationHours: number;
}

const WARNING_SEEDS: WarningSeed[] = [
  {
    type: "HEAVY_RAINFALL",
    severity: "HIGH",
    regionId: "bengaluru-urban",
    title: "Heavy Rainfall Warning — Bengaluru Urban",
    description:
      "Persistent heavy rainfall expected with waterlogging in low-lying areas including Silk Board junction and Madiwala Lake surroundings.",
    startInHours: 2,
    durationHours: 8,
  },
  {
    type: "THUNDERSTORM",
    severity: "MEDIUM",
    regionId: "bengaluru-urban",
    title: "Severe Thunderstorm Watch — Bengaluru",
    description:
      "Thunderstorms with gusty winds (40–50 km/h) expected this evening. Power interruptions possible.",
    startInHours: 4,
    durationHours: 4,
  },
  {
    type: "CYCLONE",
    severity: "CRITICAL",
    regionId: "odisha-coast",
    title: "Cyclone Emergency — Odisha Coast",
    description:
      "A severe cyclonic storm is expected to cross the Puri coast with squally winds of 90–100 km/h. Coastal evacuation advisories are in effect.",
    startInHours: 6,
    durationHours: 24,
  },
  {
    type: "FLOOD",
    severity: "HIGH",
    regionId: "north-bihar",
    title: "Flood Warning — North Bihar",
    description:
      "River levels above the danger mark expected along the Bagmati and Adhwara group of rivers. Low-lying settlements should prepare to move.",
    startInHours: 12,
    durationHours: 48,
  },
  {
    type: "HEATWAVE",
    severity: "MEDIUM",
    regionId: "delhi-ncr",
    title: "Heatwave Watch — Delhi NCR",
    description:
      "Day temperatures likely to reach 43–45°C over the next two days. Avoid outdoor exposure during peak afternoon hours.",
    startInHours: 24,
    durationHours: 48,
  },
  {
    type: "LANDSLIDE",
    severity: "HIGH",
    regionId: "kerala-western-ghats",
    title: "Landslide Warning — Wayanad, Western Ghats",
    description:
      "Saturated slopes after intense monsoon rain. Movement on the Kalpetta–Meppadi ghat road should be avoided overnight.",
    startInHours: 3,
    durationHours: 18,
  },
  {
    type: "FLOOD",
    severity: "MEDIUM",
    regionId: "chennai",
    title: "Flood Watch — Chennai",
    description:
      "Waterlogging likely in low-lying localities as the drainage system receives continuous inflow. Avoid basement parking and subways.",
    startInHours: 8,
    durationHours: 12,
  },
  {
    type: "HEAVY_RAINFALL",
    severity: "LOW",
    regionId: "mumbai-coastal",
    title: "Heavy Rainfall Advisory — Mumbai",
    description:
      "Moderate to heavy showers expected with the possibility of localized waterlogging in low-lying areas during high tide.",
    startInHours: 5,
    durationHours: 9,
  },
];

/**
 * Seeds the simulated warning feed once. Warnings are seeded with timing
 * relative to "now" so a fresh demo always shows a mix of advance warnings
 * and an active one. Skipped when warnings already exist.
 */
export async function seedSimulatedWarnings(): Promise<void> {
  const warningCount = await Warning.estimatedDocumentCount();
  if (warningCount > 0) {
    console.log(`Simulated warnings already seeded (${warningCount})`);
    return;
  }

  const now = Date.now();
  const docs = WARNING_SEEDS.map((seed) => {
    const region = findRegion(seed.regionId);
    if (!region) throw new Error(`Unknown region in warning seed: ${seed.regionId}`);

    const expectedStartAt = new Date(now + seed.startInHours * 3_600_000);
    const expectedEndAt = new Date(expectedStartAt.getTime() + seed.durationHours * 3_600_000);

    return {
      source: "Meteorological Alert Simulator (DEMO)",
      type: seed.type,
      severity: seed.severity,
      title: seed.title,
      description: seed.description,
      regionId: region.id,
      regionName: region.name,
      latitude: region.latitude,
      longitude: region.longitude,
      radiusKm: region.radiusKm,
      issuedAt: new Date(now),
      expectedStartAt,
      expectedEndAt,
      instructions: DISASTER_INSTRUCTIONS[seed.type],
      status: "ACTIVE" as const,
    };
  });

  await Warning.insertMany(docs);
  console.log(`Seeded ${docs.length} simulated meteorological warnings (DEMO DATA)`);
}
