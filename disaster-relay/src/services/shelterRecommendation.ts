import Warning from "../models/Warning";
import SafeZone, { type SafeZoneDoc } from "../models/SafeZone";
import type { WarningDisasterType, WarningSeverity } from "../data/disasterCatalog";

/** Walking pace used for travel-time estimates (prototype, not routing). */
const WALKING_KM_PER_HOUR = 4.5;

const SEVERITY_RANK: Record<WarningSeverity, number> = {
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  CRITICAL: 4,
};

/** Disasters where travelling to a shelter is appropriate at all. */
const TRAVEL_WORTHY_TYPES = new Set<WarningDisasterType>([
  "FLOOD",
  "HEAVY_RAINFALL",
  "CYCLONE",
  "LANDSLIDE",
  "TSUNAMI",
  "EARTHQUAKE",
]);

export function haversineKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.sqrt(a));
}

export function estimateTravelMinutes(km: number): number {
  return Math.max(1, Math.round((km / WALKING_KM_PER_HOUR) * 60));
}

export interface ShelterRecommendation {
  zoneId: string;
  name: string;
  kind: SafeZoneDoc["kind"];
  latitude: number;
  longitude: number;
  distanceKm: number;
  travelMinutes: number;
  facilities: string[];
  suitableFor: string[];
  accessible: boolean;
  operatingStatus: SafeZoneDoc["operatingStatus"];
  /** 0–1 simulated occupancy; null when capacity unknown. */
  occupancy: number | null;
  lastVerifiedAt: string | null;
  /** Human explanation of why (or why not) this shelter was picked. */
  reason: string;
  /** True for the single best candidate. */
  recommended: boolean;
}

export type RecommendationAdvice =
  | "EVACUATE"
  | "MONITOR_STAY_PUT"
  | "NO_ACTION";

export interface ShelterRecommendationResult {
  /** Why the app is (or is not) telling the user to move. */
  advice: RecommendationAdvice;
  adviceReason: string;
  relevantWarning: {
    id: string;
    type: WarningDisasterType;
    severity: WarningSeverity;
    title: string;
    regionName: string;
  } | null;
  recommendations: ShelterRecommendation[];
  /** Honest prototype note: occupancy is simulated demo data. */
  simulatedData: boolean;
}

interface ScoredShelter {
  doc: SafeZoneDoc;
  distanceKm: number;
  score: number;
  suitable: boolean;
  hasSpace: boolean;
  reasons: string[];
}

function scoreShelter(
  doc: SafeZoneDoc,
  distanceKm: number,
  warningType: WarningDisasterType | null,
  criticalSeverity: boolean
): ScoredShelter {
  const suitable =
    warningType == null || doc.suitableFor.includes(warningType);
  const hasSpace =
    doc.capacity == null || doc.currentOccupancy < doc.capacity;

  let score = 0;
  const reasons: string[] = [];

  if (doc.operatingStatus === "CLOSED") {
    return { doc, distanceKm, score: -1, suitable, hasSpace, reasons: ["Closed"] };
  }
  if (doc.operatingStatus === "FULL" || !hasSpace) {
    score -= 40;
    reasons.push("At capacity");
  }
  if (warningType != null) {
    if (suitable) {
      score += 50;
      reasons.push(`Suitable for ${warningType.replace("_", " ").toLowerCase()}`);
    } else {
      score -= 30;
    }
  }
  if (doc.capacity != null && hasSpace) {
    const freePct = Math.round(
      ((doc.capacity - doc.currentOccupancy) / doc.capacity) * 100
    );
    reasons.push(`${freePct}% capacity available`);
  }

  // Distance dominates when suitability ties — nearest first.
  score -= distanceKm * 6;
  if (distanceKm <= 2) reasons.push(`${distanceKm.toFixed(1)} km away`);
  if (criticalSeverity && suitable && hasSpace) score += 10;

  if (doc.facilities.includes("MEDICAL_ASSISTANCE")) reasons.push("Medical assistance on site");
  if (doc.facilities.includes("DRINKING_WATER")) reasons.push("Drinking water");
  if (doc.facilities.includes("EMERGENCY_SUPPLIES")) reasons.push("Emergency supplies");
  if (doc.accessible) reasons.push("Accessible");

  return { doc, distanceKm, score, suitable, hasSpace, reasons };
}

function buildReason(shelter: ScoredShelter, warningType: WarningDisasterType | null): string {
  if (shelter.reasons.length > 0) return shelter.reasons.join(" · ");
  return warningType == null
    ? "Nearest open shelter"
    : "Open and reachable";
}

/**
 * Recommends shelters for a user position, optionally in the context of the
 * most relevant active warning. Deliberately conservative: with no relevant
 * warning it tells the user to stay put rather than sending them walking.
 */
export async function recommendShelters(
  latitude: number,
  longitude: number,
  warningId?: string
): Promise<ShelterRecommendationResult> {
  // Pick the relevant warning: explicit id, else the closest active zone.
  let warning: Awaited<ReturnType<typeof Warning.findOne>> | null = null;
  if (warningId != null && warningId.length > 0) {
    warning = await Warning.findById(warningId).catch(() => null);
    if (warning == null || !warning.isActive()) {
      warning = null;
    }
  } else {
    const actives = await Warning.find({
      status: "ACTIVE",
      expectedEndAt: { $gt: new Date() },
    });
    const inside = actives
      .filter(
        (w) =>
          haversineKm(latitude, longitude, w.latitude, w.longitude) <=
          w.radiusKm + 2
      )
      .sort(
        (a, b) =>
          haversineKm(latitude, longitude, a.latitude, a.longitude) -
          haversineKm(latitude, longitude, b.latitude, b.longitude)
      );
    warning = inside[0] ?? null;
  }

  const zones = await SafeZone.find().lean<SafeZoneDoc[]>();
  const candidates = zones.map((raw) => {
    // Legacy documents predate some schema fields, and lean() queries skip
    // defaults — normalize before scoring so old data cannot crash the route.
    const doc = {
      ...raw,
      facilities: Array.isArray(raw.facilities) ? raw.facilities : [],
      suitableFor: Array.isArray(raw.suitableFor) ? raw.suitableFor : [],
      accessible: raw.accessible ?? false,
      operatingStatus: raw.operatingStatus ?? "OPEN",
      currentOccupancy: raw.currentOccupancy ?? 0,
    } as SafeZoneDoc;
    const distanceKm = haversineKm(latitude, longitude, doc.latitude, doc.longitude);
    return scoreShelter(
      doc,
      distanceKm,
      warning ? warning.type : null,
      warning ? warning.severity === "CRITICAL" : false
    );
  });

  candidates.sort((a, b) => b.score - a.score || a.distanceKm - b.distanceKm);

  const recommendations: ShelterRecommendation[] = candidates
    .filter((c) => c.score > -50)
    .slice(0, 3)
    .map((c, index) => ({
      zoneId: String(c.doc._id),
      name: c.doc.name,
      kind: c.doc.kind,
      latitude: c.doc.latitude,
      longitude: c.doc.longitude,
      distanceKm: Number(c.distanceKm.toFixed(2)),
      travelMinutes: estimateTravelMinutes(c.distanceKm),
      facilities: c.doc.facilities,
      suitableFor: c.doc.suitableFor,
      accessible: c.doc.accessible,
      operatingStatus: c.doc.operatingStatus,
      occupancy:
        c.doc.capacity != null && c.doc.capacity > 0
          ? Number((c.doc.currentOccupancy / c.doc.capacity).toFixed(2))
          : null,
      lastVerifiedAt: c.doc.lastVerifiedAt
        ? c.doc.lastVerifiedAt.toISOString()
        : null,
      reason: buildReason(c, warning ? warning.type : null),
      recommended: index === 0,
    }));

  let advice: RecommendationAdvice = "NO_ACTION";
  let adviceReason = "No active warnings for your area. No evacuation required.";

  if (warning) {
    const severe = SEVERITY_RANK[warning.severity] >= SEVERITY_RANK.HIGH;
    const travelWorthy = TRAVEL_WORTHY_TYPES.has(warning.type);
    if (severe && travelWorthy && recommendations.length > 0) {
      advice = "EVACUATE";
      adviceReason = `${warning.title} is active in your area. Move to the recommended shelter while routes are open.`;
    } else if (!travelWorthy) {
      advice = "MONITOR_STAY_PUT";
      adviceReason = `${warning.title} is active. For this disaster, staying indoors is safer than travelling to a shelter.`;
    } else {
      advice = "MONITOR_STAY_PUT";
      adviceReason = `${warning.title} is active in your area. Stay alert; shelters are listed if conditions worsen.`;
    }
  }

  return {
    advice,
    adviceReason,
    relevantWarning: warning
      ? {
          id: String(warning._id),
          type: warning.type,
          severity: warning.severity,
          title: warning.title,
          regionName: warning.regionName,
        }
      : null,
    recommendations,
    simulatedData: true,
  };
}
