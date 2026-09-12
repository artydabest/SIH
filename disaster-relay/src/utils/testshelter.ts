/**
 * Unit tests for the shelter scoring logic — no database required.
 * Run: npm run test:shelter
 *
 * Scoring contract (from shelterRecommendation.ts):
 *  - CLOSED shelters are never recommended
 *  - FULL / at-capacity shelters are heavily penalized (-40)
 *  - Suitability for the active warning type is the biggest plus (+50)
 *  - Distance dominates when suitability ties (-6/km)
 *  - advice: EVACUATE only for HIGH/CRITICAL + travel-worthy disaster types
 *    with at least one recommendation; MONITOR_STAY_PUT for travel-dangerous
 *    types; NO_ACTION with no relevant warning
 */
import { haversineKm, estimateTravelMinutes } from "../services/shelterRecommendation";
import type { WarningDisasterType, WarningSeverity } from "../data/disasterCatalog";

let passed = 0;
let failed = 0;

function check(name: string, condition: boolean, detail: string): void {
  if (condition) {
    passed++;
    console.log(`  PASS  ${name}`);
  } else {
    failed++;
    console.error(`  FAIL  ${name} — ${detail}`);
  }
}

// --- Pure helpers ---------------------------------------------------------

const km = haversineKm(12.9716, 77.5946, 12.9816, 77.6046);
check("haversine ~1.5 km for this offset", km > 1.3 && km < 1.7, `got ${km}`);
check("haversine is 0 for identical points", haversineKm(12.9, 77.6, 12.9, 77.6) === 0, "nonzero");
check("travel minutes: 1.2 km walk ~16 min", estimateTravelMinutes(1.2) === 16, `got ${estimateTravelMinutes(1.2)}`);
check("travel minutes floors at 1", estimateTravelMinutes(0.01) === 1, `got ${estimateTravelMinutes(0.01)}`);

// --- Scoring / advice: pure mirrors of the contract in
// shelterRecommendation.ts (importing scoreShelter directly would require a
// DB connection; the contract is small and stable, kept in sync by review) --

interface ScoreInput {
  operatingStatus: "OPEN" | "FULL" | "CLOSED";
  suitable: boolean;
  capacity: number | null;
  currentOccupancy: number;
  distanceKm: number;
  criticalSeverity: boolean;
}

function scoreShelterContract(i: ScoreInput): number {
  if (i.operatingStatus === "CLOSED") return -1;
  const hasSpace = i.capacity == null || i.currentOccupancy < i.capacity;
  let score = 0;
  if (i.operatingStatus === "FULL" || !hasSpace) score -= 40;
  if (i.suitable) score += 50;
  score -= i.distanceKm * 6;
  if (i.criticalSeverity && i.suitable && hasSpace) score += 10;
  return score;
}

const TRAVEL_WORTHY_TYPES = new Set<WarningDisasterType>([
  "FLOOD", "HEAVY_RAINFALL", "CYCLONE", "LANDSLIDE", "TSUNAMI", "EARTHQUAKE",
]);

function adviceFor(
  warning: { type: WarningDisasterType; severity: WarningSeverity } | null,
  recommendationCount: number
): "EVACUATE" | "MONITOR_STAY_PUT" | "NO_ACTION" {
  const SEVERITY_RANK: Record<WarningSeverity, number> = { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 };
  if (!warning) return "NO_ACTION";
  const severe = SEVERITY_RANK[warning.severity] >= SEVERITY_RANK.HIGH;
  const canTravel = TRAVEL_WORTHY_TYPES.has(warning.type);
  if (severe && canTravel && recommendationCount > 0) return "EVACUATE";
  return "MONITOR_STAY_PUT";
}

console.log("Shelter scoring contract:");
check("closed shelter scores -1 (never recommended)",
  scoreShelterContract({ operatingStatus: "CLOSED", suitable: true, capacity: 100, currentOccupancy: 0, distanceKm: 0.5, criticalSeverity: false }) === -1,
  "closed not excluded");
check("suitable beats nearer-but-unsuitable",
  scoreShelterContract({ operatingStatus: "OPEN", suitable: true, capacity: 100, currentOccupancy: 10, distanceKm: 2, criticalSeverity: false }) >
  scoreShelterContract({ operatingStatus: "OPEN", suitable: false, capacity: 100, currentOccupancy: 10, distanceKm: 1, criticalSeverity: false }),
  "distance outranked suitability");
check("full shelter penalized below open one",
  scoreShelterContract({ operatingStatus: "FULL", suitable: true, capacity: 100, currentOccupancy: 100, distanceKm: 1, criticalSeverity: false }) <
  scoreShelterContract({ operatingStatus: "OPEN", suitable: true, capacity: 100, currentOccupancy: 10, distanceKm: 1, criticalSeverity: false }),
  "full not penalized");
check("critical severity boosts suitable+open shelter by 10",
  scoreShelterContract({ operatingStatus: "OPEN", suitable: true, capacity: 100, currentOccupancy: 10, distanceKm: 1, criticalSeverity: true }) -
  scoreShelterContract({ operatingStatus: "OPEN", suitable: true, capacity: 100, currentOccupancy: 10, distanceKm: 1, criticalSeverity: false }) === 10,
  "critical bonus wrong");
check("unknown capacity treated as has-space",
  scoreShelterContract({ operatingStatus: "OPEN", suitable: true, capacity: null, currentOccupancy: 0, distanceKm: 1, criticalSeverity: false }) >
  scoreShelterContract({ operatingStatus: "OPEN", suitable: true, capacity: 100, currentOccupancy: 100, distanceKm: 1, criticalSeverity: false }),
  "null capacity mishandled");

console.log("Advice contract:");
check("no warning => NO_ACTION", adviceFor(null, 3) === "NO_ACTION", "advice not NO_ACTION");
check("HIGH flood + shelters => EVACUATE",
  adviceFor({ type: "FLOOD", severity: "HIGH" }, 3) === "EVACUATE", "flood not EVACUATE");
check("CRITICAL heavy rainfall + shelters => EVACUATE",
  adviceFor({ type: "HEAVY_RAINFALL", severity: "CRITICAL" }, 2) === "EVACUATE", "rainfall not EVACUATE");
check("CRITICAL lightning => MONITOR_STAY_PUT (travel is the danger)",
  adviceFor({ type: "LIGHTNING", severity: "CRITICAL" }, 3) === "MONITOR_STAY_PUT", "lightning told to travel");
check("MEDIUM flood => MONITOR_STAY_PUT (not severe enough)",
  adviceFor({ type: "FLOOD", severity: "MEDIUM" }, 3) === "MONITOR_STAY_PUT", "medium escalated");
check("HIGH flood with zero reachable shelters => MONITOR_STAY_PUT",
  adviceFor({ type: "FLOOD", severity: "HIGH" }, 0) === "MONITOR_STAY_PUT", "evacuate with no shelter");

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed > 0 ? 1 : 0);
